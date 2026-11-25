package com.leaseflow.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Request body for {@code POST /api/v1/leases/{id}/settle}. */
@Getter @Setter
public class SettlementRequest {

    @NotNull(message = "Damages amount is required (use 0.00 if none)")
    @DecimalMin(value = "0.00", message = "Damages amount cannot be negative")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal damagesAmount;

    @Size(max = 500)
    private String notes;
}

