package com.leaseflow.dto.response;

import com.leaseflow.entity.Unit;
import com.leaseflow.enums.UnitStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class UnitResponse {

    private Long          id;
    private String        unitNumber;
    private String        floor;
    private UnitStatus    status;
    private Long          propertyId;
    private String        propertyName;
    private LocalDateTime createdAt;

    public static UnitResponse from(Unit u) {
        return UnitResponse.builder()
                .id(u.getId())
                .unitNumber(u.getUnitNumber())
                .floor(u.getFloor())
                .status(u.getStatus())
                .propertyId(u.getProperty().getId())
                .propertyName(u.getProperty().getName())
                .createdAt(u.getCreatedAt())
                .build();
    }
}

