package com.lld.urlshortener.cache;

import com.lld.urlshortener.models.UrlMapping;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache service (simulates Redis)
 *
 * Cache-aside pattern:
 * 1. Check cache first
 * 2. On miss, query database
 * 3. Store in cache
 * 4. Return result
 */
public class CacheService {
    private final Map<String, UrlMapping> cache;
    private final Map<String, LocalDateTime> expiryTimes;

    public CacheService() {
        this.cache = new ConcurrentHashMap<>();
        this.expiryTimes = new ConcurrentHashMap<>();
    }

    public void put(String shortCode, UrlMapping mapping) {
        cache.put(shortCode, mapping);

        // Set TTL based on URL expiry
        if (mapping.getExpiryTime() != null) {
            expiryTimes.put(shortCode, mapping.getExpiryTime());
        } else {
            // Default TTL: 1 year
            expiryTimes.put(shortCode, LocalDateTime.now().plusYears(1));
        }
    }

    public UrlMapping get(String shortCode) {
        // Check if expired
        LocalDateTime expiry = expiryTimes.get(shortCode);
        if (expiry != null && expiry.isBefore(LocalDateTime.now())) {
            // Expired - remove from cache
            invalidate(shortCode);
            return null;
        }

        return cache.get(shortCode);
    }

    public void invalidate(String shortCode) {
        cache.remove(shortCode);
        expiryTimes.remove(shortCode);
    }

    public void clear() {
        cache.clear();
        expiryTimes.clear();
    }

    public int size() {
        return cache.size();
    }

    public boolean contains(String shortCode) {
        return cache.containsKey(shortCode) && get(shortCode) != null;
    }
}
