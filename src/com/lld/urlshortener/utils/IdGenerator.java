package com.lld.urlshortener.utils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * ID Generator for creating unique numeric IDs
 *
 * In production, this would be:
 * - Database auto-increment
 * - Twitter Snowflake (distributed ID generation)
 * - UUID (but would need different encoding)
 *
 * For demo: Simple atomic counter
 */
public class IdGenerator {
    private static IdGenerator instance;
    private final AtomicLong counter;

    private IdGenerator() {
        this.counter = new AtomicLong(1000);  // Start from 1000 for demo
    }

    public static synchronized IdGenerator getInstance() {
        if (instance == null) {
            instance = new IdGenerator();
        }
        return instance;
    }

    /**
     * Generate next unique ID
     * Thread-safe using AtomicLong
     */
    public long generateId() {
        return counter.incrementAndGet();
    }

    /**
     * Reset counter (for testing only)
     */
    public void reset() {
        counter.set(1000);
    }

    /**
     * Get current counter value
     */
    public long getCurrentValue() {
        return counter.get();
    }
}
