package com.leaseflow.dto.response;

import com.leaseflow.entity.LeaseStatusHistory;
import com.leaseflow.enums.LeaseStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LeaseStatusHistoryResponse {
    private Long id;
    private Long leaseId;
    private LeaseStatus fromStatus;
    private LeaseStatus toStatus;
    private Long changedBy;
    private LocalDateTime changedAt;
    private String notes;

    public static LeaseStatusHistoryResponse from(LeaseStatusHistory h) {
        return LeaseStatusHistoryResponse.builder()
                .id(h.getId())
                .leaseId(h.getLease() != null ? h.getLease().getId() : null)
                .fromStatus(h.getFromStatus())
                .toStatus(h.getToStatus())
                .changedBy(h.getChangedBy())
                .changedAt(h.getChangedAt())
                .notes(h.getNotes())
                .build();
    }
}

