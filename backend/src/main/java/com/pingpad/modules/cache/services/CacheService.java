package com.pingpad.modules.cache.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing Redis cache operations.
 * Implements a hybrid caching pattern:
 * - Cache-Aside (Lazy Loading) for reads: Check cache first, if miss, load from DB and populate cache
 * - Write-Through for writes: Update DB, then immediately update cache with new data
 * 
 * This hybrid approach provides:
 * - Fast reads for frequently accessed data (Cache-Aside)
 * - Immediate cache updates after writes, eliminating cache misses on subsequent reads (Write-Through)
 * - Better performance than pure Cache-Aside with invalidation-only strategy
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    /**
     * Get a value from cache by key.
     *
     * @param key Cache key
     * @param type Expected type of the cached value
     * @return Optional containing the value if found, empty otherwise
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Cache hit for key: {}", key);
                return Optional.of((T) value);
            }
            log.debug("Cache miss for key: {}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error reading from cache for key: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * Put a value into cache with default TTL.
     *
     * @param key Cache key
     * @param value Value to cache
     */
    public void put(String key, Object value) {
        put(key, value, DEFAULT_TTL);
    }

    /**
     * Put a value into cache with custom TTL.
     *
     * @param key Cache key
     * @param value Value to cache
     * @param ttl Time to live
     */
    public void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl.toSeconds(), TimeUnit.SECONDS);
            log.debug("Cached value for key: {} with TTL: {}", key, ttl);
        } catch (Exception e) {
            log.warn("Error writing to cache for key: {}", key, e);
        }
    }

    /**
     * Delete a value from cache.
     *
     * @param key Cache key
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Deleted cache key: {}", key);
        } catch (Exception e) {
            log.warn("Error deleting from cache for key: {}", key, e);
        }
    }

    /**
     * Delete multiple values from cache by pattern.
     * Useful for invalidating all cache entries matching a pattern (e.g., all endpoints for a user).
     *
     * @param pattern Pattern to match (e.g., "endpoint:user:123:*")
     */
    public void deleteByPattern(String pattern) {
        try {
            redisTemplate.delete(redisTemplate.keys(pattern));
            log.debug("Deleted cache keys matching pattern: {}", pattern);
        } catch (Exception e) {
            log.warn("Error deleting from cache by pattern: {}", pattern, e);
        }
    }

    /**
     * Check if a key exists in cache.
     *
     * @param key Cache key
     * @return true if key exists, false otherwise
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Error checking cache existence for key: {}", key, e);
            return false;
        }
    }

    /**
     * Cache a list of values.
     *
     * @param key Cache key
     * @param values List of values to cache
     */
    public void putList(String key, List<?> values) {
        put(key, values);
    }

    /**
     * Get a list of values from cache.
     *
     * @param key Cache key
     * @param elementType Type of list elements
     * @return Optional containing the list if found, empty otherwise
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<List<T>> getList(String key, Class<T> elementType) {
        Optional<List> cachedList = get(key, List.class);
        return cachedList.map(list -> (List<T>) list);
    }
}
