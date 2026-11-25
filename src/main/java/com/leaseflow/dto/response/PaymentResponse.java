package com.leaseflow.dto.response;

import com.leaseflow.entity.Payment;
import com.leaseflow.enums.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponse {
    private Long id;
    private Long invoiceId;
    private BigDecimal amountPaid;
    private String transactionReference;
    private PaymentMethod paymentMethod;
    private LocalDateTime paidAt;

    public static PaymentResponse from(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .invoiceId(p.getInvoice() != null ? p.getInvoice().getId() : null)
                .amountPaid(p.getAmountPaid())
                .transactionReference(p.getTransactionReference())
                .paymentMethod(p.getPaymentMethod())
                .paidAt(p.getPaidAt())
                .build();
    }
}

