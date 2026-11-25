package com.leaseflow.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SettlementResponse {
    private Long leaseId;
    private BigDecimal securityDeposit;
    private BigDecimal unpaidInvoiceBalance;
    private BigDecimal damagesAmount;
    private BigDecimal refundAmount;
    private boolean tenantOwes;
    private String notes;
}

