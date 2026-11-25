package com.leaseflow.enums;

/**
 * Occupancy status of a rental unit.
 * Transitions: VACANT -> OCCUPIED on lease activation; OCCUPIED -> VACANT on termination.
 */
public enum UnitStatus {
    VACANT,
    OCCUPIED
}

