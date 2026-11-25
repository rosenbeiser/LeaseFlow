package com.leaseflow.dto.request;

import com.leaseflow.enums.PropertyType;
import com.leaseflow.enums.UtilityType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Request body for {@code POST /api/v1/billing/estimate}. */
@Getter @Setter
public class BillingEstimateRequest {

    @NotNull(message = "Property type is required")
    private PropertyType propertyType;

    @NotNull(message = "Utility type is required")
    private UtilityType utilityType;

    @NotNull(message = "Units consumed is required")
    @DecimalMin(value = "0.00", message = "Units consumed cannot be negative")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal units;
}

