package com.leaseflow.dto.response;

import com.leaseflow.entity.InvoiceLineItem;
import com.leaseflow.enums.UtilityType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class InvoiceLineItemResponse {
    private Long id;
    private String description;
    private UtilityType utilityType;
    private BigDecimal unitsConsumed;
    private BigDecimal amount;

    public static InvoiceLineItemResponse from(InvoiceLineItem item) {
        return InvoiceLineItemResponse.builder()
                .id(item.getId())
                .description(item.getDescription())
                .utilityType(item.getUtilityType())
                .unitsConsumed(item.getUnitsConsumed())
                .amount(item.getAmount())
                .build();
    }
}

