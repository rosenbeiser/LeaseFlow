package com.leaseflow.enums;

/**
 * Classification of a property for billing strategy selection.
 * COMMERCIAL properties use the CommercialUtilityStrategy regardless of utility type.
 * RESIDENTIAL properties use type-specific strategies (Tiered electricity, FlatRate water).
 */
public enum PropertyType {
    RESIDENTIAL,
    COMMERCIAL
}

