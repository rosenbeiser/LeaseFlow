package com.leaseflow.dto.response;

import com.leaseflow.entity.Invoice;
import com.leaseflow.enums.InvoiceStatus;
import com.leaseflow.service.InvoiceService;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class InvoiceResponse {
    private Long id;
    private Long leaseId;
    private Long unitId;
    private String unitNumber;
    private Long propertyId;
    private String propertyName;
    private Long tenantId;
    private String tenantName;
    private String billingMonth;
    private BigDecimal baseRentAmount;
    private BigDecimal utilityTotalAmount;
    private BigDecimal fineAmount;
    private BigDecimal totalPayable;
    private BigDecimal amountPaid;
    private BigDecimal outstandingBalance;
    private InvoiceStatus status;
    private LocalDate dueDate;
    private List<InvoiceLineItemResponse> lineItems;
    private List<PaymentResponse> payments;
    private LocalDateTime createdAt;

    public static InvoiceResponse from(Invoice invoice) {
        BigDecimal outstanding = invoice.getTotalPayable() != null && invoice.getAmountPaid() != null
                ? invoice.getTotalPayable().subtract(invoice.getAmountPaid())
                : BigDecimal.ZERO;

        String monthFormatted = invoice.getBillingMonth() != null
                ? InvoiceService.fromMonthInt(invoice.getBillingMonth()).toString()
                : null;

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .leaseId(invoice.getLease() != null ? invoice.getLease().getId() : null)
                .unitId(invoice.getLease() != null && invoice.getLease().getUnit() != null ? invoice.getLease().getUnit().getId() : null)
                .unitNumber(invoice.getLease() != null && invoice.getLease().getUnit() != null ? invoice.getLease().getUnit().getUnitNumber() : null)
                .propertyId(invoice.getLease() != null && invoice.getLease().getUnit() != null && invoice.getLease().getUnit().getProperty() != null
                        ? invoice.getLease().getUnit().getProperty().getId() : null)
                .propertyName(invoice.getLease() != null && invoice.getLease().getUnit() != null && invoice.getLease().getUnit().getProperty() != null
                        ? invoice.getLease().getUnit().getProperty().getName() : null)
                .tenantId(invoice.getLease() != null && invoice.getLease().getTenant() != null ? invoice.getLease().getTenant().getId() : null)
                .tenantName(invoice.getLease() != null && invoice.getLease().getTenant() != null ? invoice.getLease().getTenant().getFullName() : null)
                .billingMonth(monthFormatted)
                .baseRentAmount(invoice.getBaseRentAmount())
                .utilityTotalAmount(invoice.getUtilityTotalAmount())
                .fineAmount(invoice.getFineAmount())
                .totalPayable(invoice.getTotalPayable())
                .amountPaid(invoice.getAmountPaid())
                .outstandingBalance(outstanding)
                .status(invoice.getStatus())
                .dueDate(invoice.getDueDate())
                .lineItems(invoice.getLineItems() != null
                        ? invoice.getLineItems().stream().map(InvoiceLineItemResponse::from).toList()
                        : List.of())
                .payments(invoice.getPayments() != null
                        ? invoice.getPayments().stream().map(PaymentResponse::from).toList()
                        : List.of())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}

