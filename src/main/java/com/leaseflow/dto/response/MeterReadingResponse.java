package com.leaseflow.dto.response;

import com.leaseflow.entity.MeterReading;
import com.leaseflow.enums.UtilityType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class MeterReadingResponse {
    private Long id;
    private Long unitId;
    private String unitNumber;
    private UtilityType utilityType;
    private BigDecimal previousReading;
    private BigDecimal currentReading;
    private BigDecimal consumption;
    private LocalDate readingDate;
    private Long recordedBy;
    private LocalDateTime createdAt;

    public static MeterReadingResponse from(MeterReading mr) {
        BigDecimal consumption = (mr.getCurrentReading() != null && mr.getPreviousReading() != null)
                ? mr.getCurrentReading().subtract(mr.getPreviousReading()).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        return MeterReadingResponse.builder()
                .id(mr.getId())
                .unitId(mr.getUnit() != null ? mr.getUnit().getId() : null)
                .unitNumber(mr.getUnit() != null ? mr.getUnit().getUnitNumber() : null)
                .utilityType(mr.getUtilityType())
                .previousReading(mr.getPreviousReading())
                .currentReading(mr.getCurrentReading())
                .consumption(consumption)
                .readingDate(mr.getReadingDate())
                .recordedBy(mr.getRecordedBy())
                .createdAt(mr.getCreatedAt())
                .build();
    }
}

