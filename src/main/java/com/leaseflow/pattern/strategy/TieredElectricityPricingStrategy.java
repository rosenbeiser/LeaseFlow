package com.leaseflow.pattern.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * <b>Concrete Strategy — Tiered Electricity Pricing (Residential)</b>
 *
 * <p>Applies a progressive (increasing) tier structure to residential electricity
 * consumption, incentivising lower usage. Rates and tier limits are configurable
 * via {@code application.yml} under {@code app.billing.electricity.*}.
 *
 * <p><b>Default tier structure:</b>
 * <pre>
 *   Tier 1: first 100 units          @ $0.10 / unit
 *   Tier 2: units 101 – 300          @ $0.15 / unit
 *   Tier 3: units above 300          @ $0.25 / unit
 * </pre>
 *
 * <p><b>Example:</b> 350 units =
 *   (100 × 0.10) + (200 × 0.15) + (50 × 0.25) = 10.00 + 30.00 + 12.50 = <b>52.50</b>
 */
@Component
public class TieredElectricityPricingStrategy implements BillingStrategy {

    private final BigDecimal tier1Limit;
    private final BigDecimal tier1Rate;
    private final BigDecimal tier2Limit;
    private final BigDecimal tier2Rate;
    private final BigDecimal tier3Rate;

    public TieredElectricityPricingStrategy(
            @Value("${app.billing.electricity.tier1-limit:100}") BigDecimal tier1Limit,
            @Value("${app.billing.electricity.tier1-rate:0.10}") BigDecimal tier1Rate,
            @Value("${app.billing.electricity.tier2-limit:300}") BigDecimal tier2Limit,
            @Value("${app.billing.electricity.tier2-rate:0.15}") BigDecimal tier2Rate,
            @Value("${app.billing.electricity.tier3-rate:0.25}") BigDecimal tier3Rate) {
        this.tier1Limit = tier1Limit;
        this.tier1Rate  = tier1Rate;
        this.tier2Limit = tier2Limit;
        this.tier2Rate  = tier2Rate;
        this.tier3Rate  = tier3Rate;
    }

    @Override
    public BigDecimal calculateUtilityCharge(BigDecimal unitsConsumed) {
        if (unitsConsumed == null || unitsConsumed.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal total = BigDecimal.ZERO;

        // Tier 1: up to tier1Limit units
        BigDecimal tier1Units = unitsConsumed.min(tier1Limit);
        total = total.add(tier1Units.multiply(tier1Rate));

        if (unitsConsumed.compareTo(tier1Limit) > 0) {
            // Tier 2: tier1Limit+1 to tier2Limit
            BigDecimal tier2Units = unitsConsumed.subtract(tier1Limit).min(tier2Limit.subtract(tier1Limit));
            total = total.add(tier2Units.multiply(tier2Rate));
        }

        if (unitsConsumed.compareTo(tier2Limit) > 0) {
            // Tier 3: above tier2Limit
            BigDecimal tier3Units = unitsConsumed.subtract(tier2Limit);
            total = total.add(tier3Units.multiply(tier3Rate));
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getDescription() {
        return String.format(
                "Tiered Electricity: first %s units @ $%s, %s–%s units @ $%s, above %s units @ $%s",
                tier1Limit, tier1Rate, tier1Limit, tier2Limit, tier2Rate, tier2Limit, tier3Rate);
    }
}

