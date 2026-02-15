package com.lld.urlshortener.services;

import com.lld.urlshortener.models.AnalyticsEvent;
import com.lld.urlshortener.models.UrlMapping;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Analytics Service - tracks URL clicks asynchronously
 *
 * Design:
 * - Events added to queue (non-blocking)
 * - Background thread processes events in batches
 * - Updates database periodically (not on every click)
 *
 * Why async?
 * - Redirect latency stays low (<10ms)
 * - Analytics not critical for redirect path
 * - Batch updates more efficient
 */
public class AnalyticsService {
    private final BlockingQueue<AnalyticsEvent> eventQueue;
    private final Map<String, Long> clickCounts;  // In-memory aggregation
    private final UrlService urlService;
    private final ExecutorService executor;
    private volatile boolean running;

    public AnalyticsService(UrlService urlService) {
        this.eventQueue = new LinkedBlockingQueue<>(10000);
        this.clickCounts = new ConcurrentHashMap<>();
        this.urlService = urlService;
        this.executor = Executors.newSingleThreadExecutor();
        this.running = true;

        // Start background processing thread
        startProcessing();
    }

    /**
     * Record a click event asynchronously
     * Non-blocking - adds to queue and returns immediately
     */
    public void recordClickAsync(String shortCode) {
        AnalyticsEvent event = new AnalyticsEvent(shortCode);

        // Non-blocking: add to queue (or drop if full)
        boolean added = eventQueue.offer(event);
        if (!added) {
            System.err.println("⚠ Analytics queue full - dropped event for: " + shortCode);
        }
    }

    /**
     * Get click count for a short code
     */
    public long getClickCount(String shortCode) {
        // Get from in-memory counts (updated by background thread)
        return clickCounts.getOrDefault(shortCode, 0L);
    }

    /**
     * Start background event processing
     */
    private void startProcessing() {
        executor.submit(() -> {
            while (running) {
                try {
                    processEvents();
                    Thread.sleep(5000);  // Process every 5 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    /**
     * Process events from queue in batches
     */
    private void processEvents() {
        // Batch processing: process up to 1000 events at a time
        int batchSize = Math.min(eventQueue.size(), 1000);
        if (batchSize == 0) {
            return;
        }

        Map<String, Long> batchCounts = new ConcurrentHashMap<>();

        // Drain events from queue
        for (int i = 0; i < batchSize; i++) {
            try {
                AnalyticsEvent event = eventQueue.poll(100, TimeUnit.MILLISECONDS);
                if (event != null) {
                    batchCounts.merge(event.getShortCode(), 1L, Long::sum);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Update in-memory counts
        batchCounts.forEach((shortCode, count) -> {
            clickCounts.merge(shortCode, count, Long::sum);

            // Update the UrlMapping object
            UrlMapping mapping = urlService.getByShortCode(shortCode);
            if (mapping != null) {
                for (int i = 0; i < count; i++) {
                    mapping.incrementClickCount();
                }
            }
        });

        if (!batchCounts.isEmpty()) {
            System.out.println("📊 Analytics: Processed " + batchSize + " events, " +
                             "updated " + batchCounts.size() + " URLs");
        }
    }

    /**
     * Shutdown the analytics service
     */
    public void shutdown() {
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get pending event count (queue size)
     */
    public int getPendingEventCount() {
        return eventQueue.size();
    }
}
