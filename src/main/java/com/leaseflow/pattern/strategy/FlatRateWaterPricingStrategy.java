package com.leaseflow.pattern.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * <b>Concrete Strategy — Flat-Rate Water Pricing (Residential)</b>
 *
 * <p>Applies a fixed per-unit rate plus a mandatory sewerage charge to residential
 * water consumption. Both values are configurable via {@code application.yml}.
 *
 * <p><b>Default formula:</b>
 * {@code charge = (unitsConsumed × ratePerUnit) + sewerageCharge}
 *
 * <p>Default rates: $1.50/unit + $5.00 sewerage surcharge.
 */
@Component
public class FlatRateWaterPricingStrategy implements BillingStrategy {

    private final BigDecimal ratePerUnit;
    private final BigDecimal sewerageCharge;

    public FlatRateWaterPricingStrategy(
            @Value("${app.billing.water.rate-per-unit:1.50}") BigDecimal ratePerUnit,
            @Value("${app.billing.water.sewerage-charge:5.00}") BigDecimal sewerageCharge) {
        this.ratePerUnit    = ratePerUnit;
        this.sewerageCharge = sewerageCharge;
    }

    @Override
    public BigDecimal calculateUtilityCharge(BigDecimal unitsConsumed) {
        if (unitsConsumed == null || unitsConsumed.compareTo(BigDecimal.ZERO) <= 0) {
            return sewerageCharge.setScale(2, RoundingMode.HALF_UP);
        }
        return unitsConsumed.multiply(ratePerUnit)
                .add(sewerageCharge)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getDescription() {
        return String.format("Flat Rate Water: $%s/unit + $%s sewerage charge", ratePerUnit, sewerageCharge);
    }
}

