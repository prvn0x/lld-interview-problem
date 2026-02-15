package com.lld.urlshortener.services;

import com.lld.urlshortener.cache.CacheService;
import com.lld.urlshortener.enums.UrlStatus;
import com.lld.urlshortener.models.UrlMapping;
import com.lld.urlshortener.utils.Base62Encoder;
import com.lld.urlshortener.utils.HashUtil;
import com.lld.urlshortener.utils.IdGenerator;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UrlService {
    private final Map<String, UrlMapping> urlsByShortCode;
    private final Map<String, UrlMapping> urlsByHash;  // For deduplication
    private final IdGenerator idGenerator;
    private final CacheService cacheService;

    public UrlService(CacheService cacheService) {
        this.urlsByShortCode = new ConcurrentHashMap<>();
        this.urlsByHash = new ConcurrentHashMap<>();
        this.idGenerator = IdGenerator.getInstance();
        this.cacheService = cacheService;
    }

    /**
     * Create a short URL for the given long URL
     *
     * Process:
     * 1. Validate URL
     * 2. Check for duplicate (same long URL)
     * 3. Generate unique ID
     * 4. Encode to Base62
     * 5. Store mapping
     * 6. Cache it
     */
    public UrlMapping createShortUrl(String longUrl, LocalDateTime expiryTime, String apiKey) {
        // 1. Validate URL
        if (!HashUtil.isValidUrl(longUrl)) {
            throw new IllegalArgumentException("Invalid URL: " + longUrl);
        }

        // 2. Check for duplicate
        String hash = HashUtil.hash(longUrl);
        UrlMapping existing = findExistingUrl(hash);
        if (existing != null && !existing.isExpired() && existing.getStatus() == UrlStatus.ACTIVE) {
            System.out.println("✓ Duplicate URL found - returning existing short code: " +
                             existing.getShortCode());
            return existing;
        }

        // 3. Generate unique ID
        long id = idGenerator.generateId();

        // 4. Encode to Base62
        String shortCode = Base62Encoder.encode(id);

        // 5. Create mapping
        UrlMapping mapping = new UrlMapping(id, shortCode, longUrl);
        mapping.setLongUrlHash(hash);
        mapping.setExpiryTime(expiryTime);
        mapping.setCreatedBy(apiKey);

        // 6. Store
        urlsByShortCode.put(shortCode, mapping);
        urlsByHash.put(hash, mapping);

        // 7. Cache
        cacheService.put(shortCode, mapping);

        System.out.println("✓ Created short URL: " + shortCode + " (ID: " + id + ")");
        return mapping;
    }

    /**
     * Create with default expiry (1 year)
     */
    public UrlMapping createShortUrl(String longUrl, String apiKey) {
        LocalDateTime defaultExpiry = LocalDateTime.now().plusYears(1);
        return createShortUrl(longUrl, defaultExpiry, apiKey);
    }

    /**
     * Get URL mapping by short code
     */
    public UrlMapping getByShortCode(String shortCode) {
        return urlsByShortCode.get(shortCode);
    }

    /**
     * Delete a short URL
     */
    public boolean deleteUrl(String shortCode) {
        UrlMapping mapping = urlsByShortCode.get(shortCode);
        if (mapping != null) {
            mapping.setStatus(UrlStatus.DELETED);
            cacheService.invalidate(shortCode);
            System.out.println("✓ Deleted short URL: " + shortCode);
            return true;
        }
        return false;
    }

    /**
     * Get all URLs created by a user
     */
    public List<UrlMapping> getUrlsByUser(String apiKey) {
        List<UrlMapping> result = new ArrayList<>();
        for (UrlMapping mapping : urlsByShortCode.values()) {
            if (apiKey.equals(mapping.getCreatedBy())) {
                result.add(mapping);
            }
        }
        return result;
    }

    /**
     * Find existing URL by hash (for deduplication)
     */
    private UrlMapping findExistingUrl(String hash) {
        return urlsByHash.get(hash);
    }

    /**
     * Get total URL count
     */
    public int getTotalUrls() {
        return urlsByShortCode.size();
    }

    /**
     * Get active URL count
     */
    public long getActiveUrls() {
        return urlsByShortCode.values().stream()
                .filter(m -> m.getStatus() == UrlStatus.ACTIVE && !m.isExpired())
                .count();
    }
}
