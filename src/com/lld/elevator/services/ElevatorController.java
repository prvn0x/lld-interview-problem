package com.lld.elevator.services;

import com.lld.elevator.enums.Direction;
import com.lld.elevator.models.Elevator;

import java.util.List;

/**
 * Elevator Controller (Dispatcher)
 *
 * Assigns best elevator for requests using scoring algorithm
 *
 * Scoring:
 * - Distance penalty: abs(currentFloor - requestFloor)
 * - Direction penalty:
 *   - 0: moving toward request
 *   - 5: idle
 *   - 20: moving away from request
 * - Load penalty: number of pending stops
 *
 * Selects elevator with minimum score
 */
public class ElevatorController {

    /**
     * Select best elevator for a request using scoring algorithm
     *
     * @param elevators List of available elevators
     * @param requestFloor Floor where request originated
     * @param direction Requested direction (UP/DOWN)
     * @return Best elevator (null if no elevators available)
     */
    public Elevator selectBestElevator(List<Elevator> elevators, int requestFloor, Direction direction) {
        if (elevators.isEmpty()) {
            return null;
        }

        Elevator bestElevator = null;
        int minScore = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            int score = elevator.calculateScore(requestFloor, direction);

            System.out.println(String.format("    Elevator-%d score: %d (floor=%d, dir=%s, pending=%d)",
                    elevator.getId(), score, elevator.getCurrentFloor(),
                    elevator.getDirection(), elevator.getPendingStops()));

            if (score < minScore) {
                minScore = score;
                bestElevator = elevator;
            }
        }

        System.out.println(String.format("    → Selected Elevator-%d with score %d",
                bestElevator.getId(), minScore));

        return bestElevator;
    }
}
