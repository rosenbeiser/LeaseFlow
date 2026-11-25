package com.leaseflow.dto.response;

import com.leaseflow.entity.Property;
import com.leaseflow.enums.PropertyType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Builder
public class PropertyResponse {

    private Long           id;
    private String         name;
    private String         address;
    private PropertyType   propertyType;
    private Long           landlordId;
    private String         landlordName;
    private List<UnitResponse> units;
    private LocalDateTime  createdAt;

    public static PropertyResponse from(Property p) {
        return PropertyResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .address(p.getAddress())
                .propertyType(p.getPropertyType())
                .landlordId(p.getLandlord().getId())
                .landlordName(p.getLandlord().getFullName())
                .createdAt(p.getCreatedAt())
                .build();
    }

    public static PropertyResponse fromWithUnits(Property p) {
        PropertyResponse r = from(p);
        // Builder is immutable — rebuild with units
        return PropertyResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .address(p.getAddress())
                .propertyType(p.getPropertyType())
                .landlordId(p.getLandlord().getId())
                .landlordName(p.getLandlord().getFullName())
                .createdAt(p.getCreatedAt())
                .units(p.getUnits().stream().map(UnitResponse::from).toList())
                .build();
    }
}

