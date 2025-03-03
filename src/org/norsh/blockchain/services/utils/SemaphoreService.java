package org.norsh.blockchain.services.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.norsh.blockchain.config.BlockchainConfig;
import org.norsh.blockchain.services.queue.CacheService;
import org.norsh.config.DefaultsConfig;
import org.norsh.exceptions.InternalException;
import org.norsh.security.Hasher;
import org.norsh.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Distributed Semaphore implementation using Redis.
 * <p>
 * This service provides a locking mechanism for synchronizing distributed processes
 * using Redis as the backend. It ensures atomic lock acquisition, expiration handling,
 * and safe release mechanisms to prevent deadlocks.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Uses Redis `SET NX EX` to ensure atomic lock acquisition.</li>
 *   <li>Applies an expiration time to avoid indefinite locks.</li>
 *   <li>Implements exponential backoff for contention handling.</li>
 *   <li>Ensures only the lock owner can release it.</li>
 *   <li>Supports forced lock release if necessary.</li>
 *   <li>Allows execution of functions within a locked context.</li>
 * </ul>
 *
 * <h2>Timeout Behavior:</h2>
 * <ul>
 *   <li>The default timeout is configured in {@link DefaultsConfig#getSemaphoreLockTimeoutMs()}.</li>
 *   <li>Custom timeouts can be specified for each lock operation.</li>
 *   <li>If a lock cannot be acquired within the timeout, it fails gracefully.</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 * @see <a href="https://docs.norsh.org">Norsh Documentation</a>
 */
@Service
public class SemaphoreService {

    private DefaultsConfig defaultsConfig = BlockchainConfig.getInstance().getDefaultsConfig();

    /** A thread-safe map for managing in-memory synchronization of semaphore locks. */
    private final Map<String, Object> synchronizedData = new ConcurrentHashMap<>();

    @Autowired
    private Log log;

    @Autowired
    private CacheService cacheService;

    /**
     * Executes an operation within a semaphore lock.
     * <p>
     * Ensures that the specified operation is executed with a lock on the given resource.
     * After execution, the lock is released.
     * </p>
     *
     * @param resourceId The unique identifier of the resource to lock.
     * @param timeoutMs  The maximum duration to wait for the lock before timing out.
     * @param function   The operation to execute within the lock.
     * @return {@code true} if the lock was successfully released after execution.
     */
    public void execute(String resourceId, Long timeoutMs, Consumer<String> function) {
        String lockId = acquireLock(resourceId, timeoutMs);

        try {
            function.accept(lockId);
        } finally {
        	release(resourceId, lockId);
		}
    }

    /**
     * Executes an operation within a semaphore lock using the default timeout.
     *
     * @param resourceId The unique identifier of the resource to lock.
     * @param function   The operation to execute within the lock.
     * @return {@code true} if the lock was successfully released after execution.
     */
    public void execute(String resourceId, Consumer<String> function) {
        execute(resourceId, defaultsConfig.getSemaphoreLockTimeoutMs(), function);
    }

    /**
     * Executes a function within a semaphore lock and returns a result.
     * <p>
     * Ensures that the function is executed with a lock on the given resource, then returns the computed result.
     * </p>
     *
     * @param <T>        The type of the result returned by the function.
     * @param resourceId The unique identifier of the resource to lock.
     * @param timeoutMs  The maximum duration to wait for the lock before timing out.
     * @param function   The function to execute within the lock.
     * @return The result of the executed function.
     */
    public <T> T execute(String resourceId, Long timeoutMs, Function<String, T> function) {
        String lockId = acquireLock(resourceId, timeoutMs);

        try {
            return function.apply(lockId);
        } finally {
            release(resourceId, lockId);
        }
    }

    /**
     * Executes a function within a semaphore lock using the default timeout.
     *
     * @param <T>        The type of the result returned by the function.
     * @param resourceId The unique identifier of the resource to lock.
     * @param function   The function to execute within the lock.
     * @return The result of the executed function.
     */
    public <T> T execute(String resourceId, Function<String, T> function) {
        return execute(resourceId, defaultsConfig.getSemaphoreLockTimeoutMs(), function);
    }

    /**
     * Captures a semaphore lock for a specific resource.
     *
     * @param resourceId The unique identifier of the resource to lock.
     * @param timeoutMs  The maximum duration to wait for the lock before timing out.
     * @return A unique lock identifier if the lock was acquired, or {@code null} if locking fails.
     */
    public String acquireLock(String resourceId, Long timeoutMs) {
        Object lock = synchronizedData.computeIfAbsent(resourceId, _ -> new Object());

        synchronized (lock) {
            try {
                String lockId = Hasher.uuid();
                long startTime = System.currentTimeMillis();
                int retries = 1;

                Boolean success;
                do {
                    success = cacheService.getTemplate().opsForValue().setIfAbsent(
                            resourceId, lockId, defaultsConfig.getSemaphoreLockTimeoutMs(), TimeUnit.MILLISECONDS);

                    if (success) { // && lockId.equals(cacheService.get(resourceId))) {
                        return lockId;
                    }

                    Thread.sleep(Math.min(defaultsConfig.getThreadInitialBackoffMs() * retries++, 
                                          defaultsConfig.getThreadMaxBackoffMs()));
                } while (Math.abs(System.currentTimeMillis() - startTime) < timeoutMs);

            } catch (Exception ex) {
                log.error("Error while capturing semaphore for resource: " + resourceId, ex);
                throw new InternalException("Failed to capture semaphore", ex);
            }

            return null;
        }
    }

    /**
     * Releases a semaphore lock for a specific resource.
     *
     * @param resourceId The unique identifier of the resource.
     * @param lockId     The unique lock identifier assigned when acquiring the lock.
     * @return {@code true} if the lock was successfully released, otherwise {@code false}.
     */
    public boolean release(String resourceId, String lockId) {
        String currentLockId = cacheService.get(resourceId);

        if (lockId.equals(currentLockId)) {
            forceRelease(resourceId);
            return true;
        } else {
            log.warning("Attempt to release lock failed: Not the lock owner for resource: " + resourceId);
            return false;
        }
    }

    /**
     * Forces the release of a semaphore lock for a specific resource.
     * <p>
     * This method should be used with caution as it removes the lock regardless of its owner.
     * It ensures that:
     * </p>
     * <ul>
     *   <li>The lock is deleted from Redis, making the resource available for other processes.</li>
     *   <li>The corresponding entry is removed from the in-memory synchronization map ({@code synchronizedData})
     *       to prevent unnecessary heap memory growth.</li>
     * </ul>
     *
     * <h2>Usage Considerations:</h2>
     * <p>
     * This method should only be used in exceptional cases where the lock needs to be forcibly released,
     * such as when a process fails to release it properly. Misuse of this method may lead to race conditions
     * or unintended overwrites.
     * </p>
     *
     * @param resourceId The unique identifier of the resource to unlock.
     */
    public void forceRelease(String resourceId) {
        cacheService.delete(resourceId);
        synchronizedData.remove(resourceId); // Prevents unnecessary heap memory growth
    }

}
