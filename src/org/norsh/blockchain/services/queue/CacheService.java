package org.norsh.blockchain.services.queue;

import org.norsh.cache.CacheStore;
import org.norsh.cache.RedisCache;
import org.norsh.util.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for cache operations using Redis.
 * <p>
 * This class extends {@link RedisCache} and implements {@link CacheStore}
 * to provide caching functionalities integrated with Redis.
 * </p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author Danthur Lice
 */
@Service
public class CacheService extends RedisCache implements CacheStore {
	public CacheService(@Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate, Logger log) {
		super(redisTemplate, log);
	}
}
