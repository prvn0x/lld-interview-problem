package com.lld.airbnb.strategies;

import com.lld.airbnb.enums.CancellationPolicy;

/**
 * Factory Pattern: Creates the appropriate cancellation strategy based on policy type
 *
 * This combines Factory Pattern with Strategy Pattern for clean object creation
 */
public class CancellationStrategyFactory {

    /**
     * Get the appropriate cancellation strategy for the given policy
     *
     * @param policy The cancellation policy type
     * @return The corresponding strategy implementation
     */
    public static CancellationStrategy getStrategy(CancellationPolicy policy) {
        switch (policy) {
            case FLEXIBLE:
                return new FlexibleCancellationStrategy();

            case MODERATE:
                return new ModerateCancellationStrategy();

            case STRICT:
                return new StrictCancellationStrategy();

            default:
                // Default to moderate policy if unknown
                return new ModerateCancellationStrategy();
        }
    }
}
