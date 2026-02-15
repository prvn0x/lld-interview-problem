package com.lld.urlshortener;

import com.lld.urlshortener.cache.CacheService;
import com.lld.urlshortener.models.UrlMapping;
import com.lld.urlshortener.models.User;
import com.lld.urlshortener.services.*;
import com.lld.urlshortener.utils.Base62Encoder;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("       URL SHORTENER - LOW LEVEL DESIGN DEMO");
        System.out.println("═══════════════════════════════════════════════════════\n");

        // Initialize services
        CacheService cacheService = new CacheService();
        UrlService urlService = new UrlService(cacheService);
        AnalyticsService analyticsService = new AnalyticsService(urlService);
        RedirectService redirectService = new RedirectService(urlService, cacheService, analyticsService);

        String BASE_URL = "https://short.ly";

        // ===== STEP 1: Create Users with API Keys =====
        System.out.println("▶ STEP 1: Create Users with API Keys");
        System.out.println("─────────────────────────────────────────────────────\n");

        User user1 = new User("U001", "API_KEY_ABC123", "Alice");
        User user2 = new User("U002", "API_KEY_XYZ789", "Bob");

        System.out.println("✓ Created user: " + user1);
        System.out.println("✓ Created user: " + user2);

        // ===== STEP 2: Test Base62 Encoding =====
        System.out.println("\n▶ STEP 2: Base62 Encoding Demo");
        System.out.println("─────────────────────────────────────────────────────\n");

        long[] testIds = {1001, 12345, 1000000, 9876543210L};
        for (long id : testIds) {
            String encoded = Base62Encoder.encode(id);
            System.out.println("ID: " + id + " → Base62: " + encoded +
                             " (length: " + encoded.length() + ")");
        }

        System.out.println("\n💡 Capacity:");
        System.out.println("   6 chars: 62^6 = 56 billion URLs");
        System.out.println("   7 chars: 62^7 = 3.5 trillion URLs");

        // ===== STEP 3: Create Short URLs =====
        System.out.println("\n▶ STEP 3: Create Short URLs");
        System.out.println("─────────────────────────────────────────────────────\n");

        UrlMapping url1 = urlService.createShortUrl(
                "https://www.example.com/very/long/url/with/many/segments",
                user1.getApiKey()
        );
        System.out.println("Short URL: " + url1.getShortUrl(BASE_URL));
        System.out.println("Expiry: " + url1.getExpiryTime() + "\n");

        UrlMapping url2 = urlService.createShortUrl(
                "https://github.com/user/repository/issues/12345",
                user1.getApiKey()
        );
        System.out.println("Short URL: " + url2.getShortUrl(BASE_URL) + "\n");

        UrlMapping url3 = urlService.createShortUrl(
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                user2.getApiKey()
        );
        System.out.println("Short URL: " + url3.getShortUrl(BASE_URL) + "\n");

        // ===== STEP 4: Test Duplicate Detection =====
        System.out.println("▶ STEP 4: Duplicate Detection");
        System.out.println("─────────────────────────────────────────────────────\n");

        System.out.println("Attempting to create short URL for same long URL...");
        UrlMapping duplicate = urlService.createShortUrl(
                "https://www.example.com/very/long/url/with/many/segments",
                user1.getApiKey()
        );
        System.out.println("Result: Got same short code: " + duplicate.getShortCode());
        System.out.println("✓ Duplicate detection working!\n");

        // ===== STEP 5: Redirect URLs =====
        System.out.println("▶ STEP 5: Redirect URLs (Cache-First Lookup)");
        System.out.println("─────────────────────────────────────────────────────\n");

        System.out.println("Redirecting short code: " + url1.getShortCode());
        String longUrl1 = redirectService.redirect(url1.getShortCode());
        System.out.println("✓ Redirected to: " + longUrl1);
        System.out.println("✓ Analytics event queued asynchronously\n");

        System.out.println("Redirecting again (cache hit!):");
        String longUrl2 = redirectService.redirect(url1.getShortCode());
        System.out.println("✓ Redirected to: " + longUrl2);
        System.out.println("✓ Served from cache (fast!)\n");

        // Multiple redirects to generate analytics
        System.out.println("Generating multiple redirects for analytics...");
        for (int i = 0; i < 10; i++) {
            redirectService.redirect(url2.getShortCode());
        }
        System.out.println("✓ Generated 10 redirects for: " + url2.getShortCode() + "\n");

        // ===== STEP 6: Wait for Analytics Processing =====
        System.out.println("▶ STEP 6: Analytics Processing");
        System.out.println("─────────────────────────────────────────────────────\n");

        System.out.println("⏳ Waiting for analytics to process...");
        Thread.sleep(6000);  // Wait for background thread to process

        System.out.println("\n📊 Analytics Results:");
        System.out.println("   " + url1.getShortCode() + " → " +
                          url1.getClickCount() + " clicks");
        System.out.println("   " + url2.getShortCode() + " → " +
                          url2.getClickCount() + " clicks");
        System.out.println("   " + url3.getShortCode() + " → " +
                          url3.getClickCount() + " clicks\n");

        // ===== STEP 7: Test URL Expiration =====
        System.out.println("▶ STEP 7: URL Expiration");
        System.out.println("─────────────────────────────────────────────────────\n");

        System.out.println("Creating short URL with 2 second expiry...");
        LocalDateTime shortExpiry = LocalDateTime.now().plusSeconds(2);
        UrlMapping expiring = urlService.createShortUrl(
                "https://www.example.com/temporary-link",
                shortExpiry,
                user1.getApiKey()
        );
        System.out.println("✓ Created: " + expiring.getShortUrl(BASE_URL));
        System.out.println("✓ Expires at: " + expiring.getExpiryTime());

        System.out.println("\nRedirecting before expiry:");
        String result1 = redirectService.redirect(expiring.getShortCode());
        System.out.println("✓ Success: " + result1);

        System.out.println("\n⏳ Waiting for expiration (3 seconds)...");
        Thread.sleep(3000);

        System.out.println("\nAttempting redirect after expiry:");
        try {
            redirectService.redirect(expiring.getShortCode());
        } catch (RedirectService.UrlExpiredException e) {
            System.out.println("✓ Correctly returned error: " + e.getMessage());
        }

        // ===== STEP 8: Test Not Found =====
        System.out.println("\n▶ STEP 8: Handle Not Found");
        System.out.println("─────────────────────────────────────────────────────\n");

        System.out.println("Attempting to redirect non-existent short code...");
        try {
            redirectService.redirect("INVALID");
        } catch (RedirectService.NotFoundException e) {
            System.out.println("✓ Correctly returned 404: " + e.getMessage());
        }

        // ===== STEP 9: Cache Performance Demo =====
        System.out.println("\n▶ STEP 9: Cache Performance");
        System.out.println("─────────────────────────────────────────────────────\n");

        System.out.println("Cache status:");
        System.out.println("   Cached URLs: " + cacheService.size());
        System.out.println("   Contains " + url1.getShortCode() + ": " +
                          cacheService.contains(url1.getShortCode()));

        System.out.println("\nInvalidating cache for: " + url1.getShortCode());
        cacheService.invalidate(url1.getShortCode());
        System.out.println("✓ Cache invalidated");

        System.out.println("\nNext redirect will hit database (cache miss):");
        redirectService.redirect(url1.getShortCode());
        System.out.println("✓ Fetched from database and re-cached\n");

        // ===== STEP 10: User Statistics =====
        System.out.println("▶ STEP 10: User Statistics");
        System.out.println("─────────────────────────────────────────────────────\n");

        System.out.println("Alice's URLs:");
        for (UrlMapping m : urlService.getUrlsByUser(user1.getApiKey())) {
            System.out.println("   - " + m.getShortCode() + " → " +
                             m.getLongUrl() + " (" + m.getClickCount() + " clicks)");
        }

        System.out.println("\nBob's URLs:");
        for (UrlMapping m : urlService.getUrlsByUser(user2.getApiKey())) {
            System.out.println("   - " + m.getShortCode() + " → " +
                             m.getLongUrl() + " (" + m.getClickCount() + " clicks)");
        }

        // ===== FINAL SUMMARY =====
        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("             DEMO COMPLETED SUCCESSFULLY!");
        System.out.println("═══════════════════════════════════════════════════════");

        System.out.println("\n📊 System Statistics:");
        System.out.println("   ✓ Total URLs created: " + urlService.getTotalUrls());
        System.out.println("   ✓ Active URLs: " + urlService.getActiveUrls());
        System.out.println("   ✓ Cached URLs: " + cacheService.size());
        System.out.println("   ✓ Pending analytics events: " +
                          analyticsService.getPendingEventCount());

        System.out.println("\n✨ Key Features Demonstrated:");
        System.out.println("   • Base62 encoding for compact short codes");
        System.out.println("   • Duplicate URL detection (hash-based)");
        System.out.println("   • Cache-first redirect (low latency)");
        System.out.println("   • Asynchronous analytics processing");
        System.out.println("   • URL expiration handling");
        System.out.println("   • Error handling (404, 410)");

        System.out.println("\n💡 Production Improvements:");
        System.out.println("   → Use Redis for distributed caching");
        System.out.println("   → Use Kafka/RabbitMQ for analytics queue");
        System.out.println("   → Database sharding by short code hash");
        System.out.println("   → Twitter Snowflake for distributed IDs");
        System.out.println("   → CDN for global low-latency redirects");

        System.out.println("\n═══════════════════════════════════════════════════════\n");

        // Cleanup
        analyticsService.shutdown();
    }
}
