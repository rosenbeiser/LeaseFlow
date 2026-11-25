package com.leaseflow.enums;

/**
 * Lease lifecycle states managed by the State Pattern.
 * Valid transitions:
 *   DRAFT -> ACTIVE -> NOTICE_SERVED -> TERMINATED -> SETTLED
 * ACTIVE can also transition directly to TERMINATED (early termination).
 */
public enum LeaseStatus {
    DRAFT,
    ACTIVE,
    NOTICE_SERVED,
    TERMINATED,
    SETTLED
}

