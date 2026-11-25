package com.leaseflow.pattern.strategy;

import java.math.BigDecimal;

/**
 * Billing Strategy — Context Interface (Strategy Pattern).
 *
 * <p>Defines the contract that every concrete billing strategy must fulfil.
 * The {@link BillingStrategyFactory} picks the right implementation at runtime
 * based on the {@code PropertyType} and {@code UtilityType} of the meter reading,
 * so the invoicing pipeline is completely decoupled from pricing knowledge.
 *
 * <h2>Why Strategy Pattern?</h2>
 * <p>Different property types (residential vs. commercial) and utility types
 * (electricity vs. water vs. gas) have fundamentally different pricing rules:
 * <ul>
 *   <li>Electricity for residential tenants uses tiered pricing (lower rate for
 *       the first N units, higher rate after).</li>
 *   <li>Water is billed at a flat rate per cubic metre regardless of volume.</li>
 *   <li>Commercial properties use a single negotiated rate for all utilities.</li>
 * </ul>
 * Putting all these rules in one {@code if/else} block would violate the
 * Open/Closed Principle. Adding a new utility type becomes a one-class change.
 *
 * <h2>Implementations</h2>
 * <ul>
 *   <li>{@link TieredElectricityPricingStrategy} — residential electricity</li>
 *   <li>{@link FlatRateWaterPricingStrategy} — residential water/gas</li>
 *   <li>{@link CommercialUtilityStrategy} — all commercial utilities</li>
 * </ul>
 *
 * @author Student 2207083
 */
public interface BillingStrategy {

    /**
     * Calculate the charge for a given consumption.
     *
     * @param unitsConsumed number of units consumed (kWh, m³, etc.)
     * @param ratePerUnit   base rate configured on the property (may be overridden
     *                      by the strategy if tiered pricing applies)
     * @return total charge as a non-null, non-negative {@link BigDecimal}
     */
    BigDecimal calculate(BigDecimal unitsConsumed, BigDecimal ratePerUnit);

    /**
     * Human-readable label for this strategy, used in invoice line-item descriptions.
     *
     * @return e.g. {@code "Tiered Electricity"}, {@code "Flat-Rate Water"}
     */
    String getStrategyName();
}
