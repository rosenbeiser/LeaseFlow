package com.leaseflow.pattern.strategy;

import com.leaseflow.enums.PropertyType;
import com.leaseflow.enums.UtilityType;
import com.leaseflow.exception.UnsupportedUtilityException;
import org.springframework.stereotype.Component;

/**
 * <b>Strategy Factory — Billing Strategy Selection</b>
 *
 * <p>Selects the appropriate {@link BillingStrategy} based on the combination of
 * {@link PropertyType} and {@link UtilityType}. This is the single authoritative
 * place where business rules about "which pricing applies to which meter" live.
 *
 * <p><b>Selection matrix:</b>
 * <pre>
 *   COMMERCIAL  + any utility         → CommercialUtilityStrategy
 *   RESIDENTIAL + ELECTRICITY         → TieredElectricityPricingStrategy
 *   RESIDENTIAL + WATER               → FlatRateWaterPricingStrategy
 *   RESIDENTIAL + GAS                 → UnsupportedUtilityException
 * </pre>
 *
 * <p>Strategies are injected as Spring beans so their {@code @Value} rate
 * properties are honoured.
 */
@Component
public class BillingStrategyFactory {

    private final TieredElectricityPricingStrategy tieredElectricity;
    private final FlatRateWaterPricingStrategy flatRateWater;
    private final CommercialUtilityStrategy commercial;

    public BillingStrategyFactory(TieredElectricityPricingStrategy tieredElectricity,
                                  FlatRateWaterPricingStrategy flatRateWater,
                                  CommercialUtilityStrategy commercial) {
        this.tieredElectricity = tieredElectricity;
        this.flatRateWater     = flatRateWater;
        this.commercial        = commercial;
    }

    /**
     * Returns the billing strategy for the given property type and utility type.
     *
     * @throws UnsupportedUtilityException if no strategy exists for the combination
     */
    public BillingStrategy getStrategy(PropertyType propertyType, UtilityType utilityType) {
        if (propertyType == PropertyType.COMMERCIAL) {
            return commercial;
        }
        // RESIDENTIAL
        return switch (utilityType) {
            case ELECTRICITY -> tieredElectricity;
            case WATER       -> flatRateWater;
            case GAS         -> throw new UnsupportedUtilityException(
                    "GAS billing is not supported for RESIDENTIAL properties.");
        };
    }
}

