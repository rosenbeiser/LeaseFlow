package com.leaseflow.pattern.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * <b>Concrete Strategy — Commercial Utility Pricing</b>
 *
 * <p>Applied to <em>all</em> utility types (electricity, water, gas) for
 * {@code COMMERCIAL} properties. Uses a fixed base charge plus a per-unit rate,
 * reflecting higher infrastructure and service costs for commercial tenants.
 *
 * <p><b>Default formula:</b>
 * {@code charge = baseCharge + (unitsConsumed × ratePerUnit)}
 *
 * <p>Defaults: $50.00 base + $0.30/unit.
 */
@Component
public class CommercialUtilityStrategy implements BillingStrategy {

    private final BigDecimal baseCharge;
    private final BigDecimal ratePerUnit;

    public CommercialUtilityStrategy(
            @Value("${app.billing.commercial.base-charge:50.00}") BigDecimal baseCharge,
            @Value("${app.billing.commercial.rate-per-unit:0.30}") BigDecimal ratePerUnit) {
        this.baseCharge  = baseCharge;
        this.ratePerUnit = ratePerUnit;
    }

    @Override
    public BigDecimal calculateUtilityCharge(BigDecimal unitsConsumed) {
        BigDecimal units = (unitsConsumed == null || unitsConsumed.compareTo(BigDecimal.ZERO) < 0)
                ? BigDecimal.ZERO : unitsConsumed;
        return baseCharge.add(units.multiply(ratePerUnit))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getDescription() {
        return String.format("Commercial Utility: $%s base + $%s/unit", baseCharge, ratePerUnit);
    }
}

