package com.leaseflow.dto.request;

import com.leaseflow.enums.UtilityType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Request body for {@code POST /api/v1/meter-readings}. */
@Getter @Setter
public class MeterReadingRequest {

    @NotNull(message = "Unit ID is required")
    private Long unitId;

    @NotNull(message = "Utility type is required")
    private UtilityType utilityType;

    /** If null, the service auto-derives this from the latest prior reading. */
    @DecimalMin(value = "0.00", message = "Previous reading cannot be negative")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal previousReading;

    @NotNull(message = "Current reading is required")
    @DecimalMin(value = "0.00", message = "Current reading cannot be negative")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal currentReading;

    @NotNull(message = "Reading date is required")
    private LocalDate readingDate;
}

