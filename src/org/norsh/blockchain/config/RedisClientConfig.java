package org.norsh.blockchain.config;

import org.norsh.cache.RedisClient;
import org.norsh.config.RedisConfig;
import org.norsh.util.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Redis configuration for blockchain operations.
 * <p>
 * This configuration extends {@link RedisClient} to initialize Redis connection settings dynamically
 * from {@link BlockchainConfig}, ensuring proper integration with the blockchain environment.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Retrieves Redis connection properties from {@link BlockchainConfig}.</li>
 *   <li>Provides a pre-configured {@link RedisTemplate} with String serializers.</li>
 *   <li>Clears sensitive Redis configuration from memory after initialization.</li>
 * </ul>
 *
 * <h2>Example Usage:</h2>
 * <pre>
 * RedisTemplate<String, String> template = redisTemplate(redisConnectionFactory());
 * </pre>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 */
@Configuration
public class RedisClientConfig extends RedisClient {

    /**
     * Initializes the Redis connection factory using the configuration from {@link BlockchainConfig}.
     * <p>
     * This method retrieves the Redis connection settings and ensures that
     * sensitive configuration data is cleared from memory after initialization.
     * </p>
     *
     * @return a configured {@link RedisConnectionFactory} instance.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(Logger log) {
        RedisConfig redisConfig = BlockchainConfig.getInstance().getRedisConfig();
        
        if (redisConfig == null) {
            throw new IllegalStateException("Redis configuration is missing. Ensure it is properly set in BlockchainConfig.");
        }
        
        RedisConnectionFactory redisConnectionFactory = super.redisConnectionFactory(redisConfig, log);

        // Clears Redis configuration from memory to enhance security
        BlockchainConfig.getInstance().clearRedisConfig();

        return redisConnectionFactory;
    }

    /**
     * Creates a {@link RedisTemplate} for String-based key-value storage.
     * <p>
     * This template ensures proper serialization of keys and values while interacting with Redis.
     * </p>
     *
     * @param connectionFactory the Redis connection factory.
     * @return a pre-configured {@link RedisTemplate} instance.
     */
    @Bean("redisTemplate")
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        return super.redisTemplate(connectionFactory);
    }
}
