package com.leaseflow.dto.response;

import com.leaseflow.enums.PropertyType;
import com.leaseflow.enums.UtilityType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BillingEstimateResponse {
    private PropertyType propertyType;
    private UtilityType utilityType;
    private BigDecimal unitsConsumed;
    private BigDecimal estimatedCharge;
    private String strategyDescription;
}

