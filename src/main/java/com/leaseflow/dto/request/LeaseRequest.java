package com.leaseflow.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Request body for {@code POST /api/v1/leases}. */
@Getter @Setter
public class LeaseRequest {

    @NotNull(message = "Unit ID is required")
    private Long unitId;

    @NotNull(message = "Tenant ID is required")
    private Long tenantId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Base rent is required")
    @DecimalMin(value = "0.01", message = "Base rent must be positive")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal baseRent;

    @NotNull(message = "Security deposit is required")
    @DecimalMin(value = "0.00", message = "Security deposit cannot be negative")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal securityDeposit;
}

