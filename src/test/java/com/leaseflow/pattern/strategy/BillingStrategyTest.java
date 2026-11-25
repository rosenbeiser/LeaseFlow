package com.leaseflow.pattern.strategy;

import com.leaseflow.enums.PropertyType;
import com.leaseflow.enums.UtilityType;
import com.leaseflow.exception.UnsupportedUtilityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Billing Strategy Pattern Tests")
class BillingStrategyTest {

    private TieredElectricityPricingStrategy electricityStrategy;
    private FlatRateWaterPricingStrategy waterStrategy;
    private CommercialUtilityStrategy commercialStrategy;
    private BillingStrategyFactory factory;

    @BeforeEach
    void setUp() {
        electricityStrategy = new TieredElectricityPricingStrategy(
                new BigDecimal("100"),
                new BigDecimal("0.10"),
                new BigDecimal("300"),
                new BigDecimal("0.15"),
                new BigDecimal("0.25")
        );

        waterStrategy = new FlatRateWaterPricingStrategy(
                new BigDecimal("1.50"),
                new BigDecimal("5.00")
        );

        commercialStrategy = new CommercialUtilityStrategy(
                new BigDecimal("50.00"),
                new BigDecimal("0.30")
        );

        factory = new BillingStrategyFactory(electricityStrategy, waterStrategy, commercialStrategy);
    }

    @ParameterizedTest(name = "Tiered electricity: {0} units = ${1}")
    @CsvSource({
            "0,   0.00",
            "100, 10.00",
            "101, 10.15",
            "300, 40.00",
            "301, 40.25",
            "350, 52.50"
    })
    @DisplayName("Tiered electricity pricing verifies exact boundary calculations")
    void testTieredElectricityBoundaries(String unitsStr, String expectedChargeStr) {
        BigDecimal units = new BigDecimal(unitsStr);
        BigDecimal expected = new BigDecimal(expectedChargeStr);

        BigDecimal actual = electricityStrategy.calculateUtilityCharge(units);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Flat rate water pricing calculates base rate plus sewerage surcharge")
    void testFlatRateWater() {
        // 0 units -> base charge 5.00
        assertEquals(new BigDecimal("5.00"), waterStrategy.calculateUtilityCharge(BigDecimal.ZERO));

        // 10 units -> (10 * 1.50) + 5.00 = 20.00
        assertEquals(new BigDecimal("20.00"), waterStrategy.calculateUtilityCharge(new BigDecimal("10")));

        // 25.5 units -> (25.5 * 1.50) + 5.00 = 38.25 + 5.00 = 43.25
        assertEquals(new BigDecimal("43.25"), waterStrategy.calculateUtilityCharge(new BigDecimal("25.5")));
    }

    @Test
    @DisplayName("Commercial utility strategy applies flat base fee plus linear per-unit rate")
    void testCommercialUtility() {
        // 0 units -> base charge 50.00
        assertEquals(new BigDecimal("50.00"), commercialStrategy.calculateUtilityCharge(BigDecimal.ZERO));

        // 100 units -> 50.00 + (100 * 0.30) = 80.00
        assertEquals(new BigDecimal("80.00"), commercialStrategy.calculateUtilityCharge(new BigDecimal("100")));
    }

    @Test
    @DisplayName("BillingStrategyFactory correctly maps property and utility combinations")
    void testStrategyFactorySelection() {
        // Commercial properties use commercial strategy for all utilities
        assertInstanceOf(CommercialUtilityStrategy.class,
                factory.getStrategy(PropertyType.COMMERCIAL, UtilityType.ELECTRICITY));
        assertInstanceOf(CommercialUtilityStrategy.class,
                factory.getStrategy(PropertyType.COMMERCIAL, UtilityType.WATER));
        assertInstanceOf(CommercialUtilityStrategy.class,
                factory.getStrategy(PropertyType.COMMERCIAL, UtilityType.GAS));

        // Residential properties map to type-specific strategies
        assertInstanceOf(TieredElectricityPricingStrategy.class,
                factory.getStrategy(PropertyType.RESIDENTIAL, UtilityType.ELECTRICITY));
        assertInstanceOf(FlatRateWaterPricingStrategy.class,
                factory.getStrategy(PropertyType.RESIDENTIAL, UtilityType.WATER));

        // Residential GAS is unsupported and must throw UnsupportedUtilityException
        assertThrows(UnsupportedUtilityException.class, () ->
                factory.getStrategy(PropertyType.RESIDENTIAL, UtilityType.GAS));
    }
}

