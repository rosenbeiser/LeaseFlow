package com.leaseflow.dto.response;

import com.leaseflow.entity.Lease;
import com.leaseflow.enums.LeaseStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class LeaseResponse {
    private Long id;
    private Long unitId;
    private String unitNumber;
    private Long propertyId;
    private String propertyName;
    private Long tenantId;
    private String tenantName;
    private String tenantEmail;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal baseRent;
    private BigDecimal securityDeposit;
    private LeaseStatus status;
    private List<String> allowedActions;
    private List<LeaseStatusHistoryResponse> statusHistory;
    private LocalDateTime createdAt;

    public static LeaseResponse from(Lease lease, List<String> allowedActions) {
        return LeaseResponse.builder()
                .id(lease.getId())
                .unitId(lease.getUnit().getId())
                .unitNumber(lease.getUnit().getUnitNumber())
                .propertyId(lease.getUnit().getProperty().getId())
                .propertyName(lease.getUnit().getProperty().getName())
                .tenantId(lease.getTenant().getId())
                .tenantName(lease.getTenant().getFullName())
                .tenantEmail(lease.getTenant().getEmail())
                .startDate(lease.getStartDate())
                .endDate(lease.getEndDate())
                .baseRent(lease.getBaseRent())
                .securityDeposit(lease.getSecurityDeposit())
                .status(lease.getStatus())
                .allowedActions(allowedActions)
                .statusHistory(lease.getStatusHistory() != null
                        ? lease.getStatusHistory().stream().map(LeaseStatusHistoryResponse::from).toList()
                        : List.of())
                .createdAt(lease.getCreatedAt())
                .build();
    }
}

