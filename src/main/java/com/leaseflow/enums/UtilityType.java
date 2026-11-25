package com.leaseflow.enums;

/**
 * Types of utility services tracked by meter readings.
 * Determines which BillingStrategy is applied during invoice generation.
 * Note: GAS is only supported for COMMERCIAL properties.
 */
public enum UtilityType {
    ELECTRICITY,
    WATER,
    GAS
}

