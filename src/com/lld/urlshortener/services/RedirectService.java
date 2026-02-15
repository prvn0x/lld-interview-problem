package com.lld.urlshortener.services;

import com.lld.urlshortener.cache.CacheService;
import com.lld.urlshortener.enums.UrlStatus;
import com.lld.urlshortener.models.UrlMapping;

/**
 * Redirect Service - handles URL redirects
 *
 * Key features:
 * - Cache-first lookup (fast!)
 * - Expiry validation
 * - Async analytics recording
 */
public class RedirectService {
    private final UrlService urlService;
    private final CacheService cacheService;
    private final AnalyticsService analyticsService;

    public RedirectService(UrlService urlService, CacheService cacheService,
                          AnalyticsService analyticsService) {
        this.urlService = urlService;
        this.cacheService = cacheService;
        this.analyticsService = analyticsService;
    }

    /**
     * Redirect short code to long URL
     *
     * Flow:
     * 1. Lookup (cache-first)
     * 2. Validate expiry
     * 3. Record analytics (async)
     * 4. Return long URL
     *
     * @return Long URL for redirect
     * @throws NotFoundException if short code not found
     * @throws UrlExpiredException if URL has expired
     */
    public String redirect(String shortCode) {
        // 1. Lookup (cache-first)
        UrlMapping mapping = lookupUrl(shortCode);

        if (mapping == null) {
            throw new NotFoundException("Short URL not found: " + shortCode);
        }

        // 2. Check expiration
        if (mapping.isExpired() || mapping.getStatus() != UrlStatus.ACTIVE) {
            throw new UrlExpiredException("Short URL has expired or is inactive: " + shortCode);
        }

        // 3. Record analytics asynchronously (fire-and-forget)
        analyticsService.recordClickAsync(shortCode);

        // 4. Return long URL
        return mapping.getLongUrl();
    }

    /**
     * Lookup URL with cache-first strategy
     *
     * Cache hit → return immediately (fast!)
     * Cache miss → query service, store in cache, return
     */
    private UrlMapping lookupUrl(String shortCode) {
        // Check cache first
        UrlMapping cached = cacheService.get(shortCode);
        if (cached != null) {
            return cached;
        }

        // Cache miss - query service
        UrlMapping mapping = urlService.getByShortCode(shortCode);
        if (mapping != null) {
            // Store in cache for next time
            cacheService.put(shortCode, mapping);
        }

        return mapping;
    }

    // Custom exceptions
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    public static class UrlExpiredException extends RuntimeException {
        public UrlExpiredException(String message) {
            super(message);
        }
    }
}
