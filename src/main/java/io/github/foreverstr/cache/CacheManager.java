package io.github.foreverstr.cache;

import io.github.foreverstr.cache.config.RedisCacheProperties;
import io.github.foreverstr.cache.redis.RedisSecondLevelCache;
import org.springframework.data.redis.core.RedisTemplate;

public class CacheManager {
    private static SecondLevelCache secondLevelCache;
    private static boolean cacheEnabled = true;

    public static void init(RedisTemplate<String, Object> redisTemplate, RedisCacheProperties properties) {
        if (properties.isEnabled()) {
            secondLevelCache = new RedisSecondLevelCache(redisTemplate, properties);
            cacheEnabled = true;
        } else {
            cacheEnabled = false;
        }
    }

    public static SecondLevelCache getSecondLevelCache() {
        return secondLevelCache;
    }

    public static boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public static void setCacheEnabled(boolean enabled) {
        cacheEnabled = enabled;
    }
}