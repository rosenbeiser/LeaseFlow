package com.leaseflow.dto.request;

import com.leaseflow.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/** Request body for {@code POST /api/v1/invoices/{id}/pay}. */
@Getter @Setter
public class PaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 10, fraction = 2)
    private java.math.BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotBlank(message = "Transaction reference is required")
    @Size(max = 100)
    private String transactionReference;
}

