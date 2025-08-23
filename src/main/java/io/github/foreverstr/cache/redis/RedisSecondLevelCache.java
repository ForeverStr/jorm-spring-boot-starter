package io.github.foreverstr.cache.redis;

import io.github.foreverstr.cache.SecondLevelCache;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

public class RedisSecondLevelCache implements SecondLevelCache {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisCacheProperties properties;

    public RedisSecondLevelCache(RedisTemplate<String, Object> redisTemplate, RedisCacheProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public void put(String region, String key, Object value) {
        if (value == null && !properties.isCacheNullValues()) {
            return;
        }

        String cacheKey = getCacheKey(region, key);
        if (properties.getDefaultExpiration() > 0) {
            redisTemplate.opsForValue().set(cacheKey, value, properties.getDefaultExpiration(), TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(cacheKey, value);
        }
    }

    @Override
    public Object get(String region, String key) {
        String cacheKey = getCacheKey(region, key);
        return redisTemplate.opsForValue().get(cacheKey);
    }

    @Override
    public void remove(String region, String key) {
        String cacheKey = getCacheKey(region, key);
        redisTemplate.delete(cacheKey);
    }

    @Override
    public void clearRegion(String region) {
        String pattern = properties.isUseKeyPrefix() ?
                properties.getKeyPrefix() + region + ":*" :
                region + ":*";

        redisTemplate.delete(redisTemplate.keys(pattern));
    }

    @Override
    public void clearAll() {
        String pattern = properties.isUseKeyPrefix() ?
                properties.getKeyPrefix() + "*" :
                "*";

        redisTemplate.delete(redisTemplate.keys(pattern));
    }

    private String getCacheKey(String region, String key) {
        if (properties.isUseKeyPrefix()) {
            return properties.getKeyPrefix() + region + ":" + key;
        }
        return region + ":" + key;
    }
}