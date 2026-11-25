package com.leaseflow.enums;

/**
 * Roles available in the LeaseFlow system.
 * ROLE_ prefix is required by Spring Security's authority naming convention.
 */
public enum Role {
    ROLE_LANDLORD,
    ROLE_TENANT,
    ROLE_ADMIN
}

