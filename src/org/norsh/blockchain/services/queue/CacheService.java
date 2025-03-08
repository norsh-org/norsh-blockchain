package org.norsh.blockchain.services.queue;

import org.norsh.blockchain.S;
import org.norsh.cache.RedisCache;

/**
 * Service responsible for cache operations using Redis.
 * <p>
 * This class extends {@link RedisCache}
 * to provide caching functionalities integrated with Redis.
 * </p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 */
public class CacheService extends RedisCache {
	public CacheService() {
		super(S.config.getRedisConfig(), S.log);
	}
}
