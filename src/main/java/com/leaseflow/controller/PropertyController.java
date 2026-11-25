package com.leaseflow.controller;

import com.leaseflow.dto.common.ApiResponse;
import com.leaseflow.dto.common.PageResponse;
import com.leaseflow.dto.request.PropertyRequest;
import com.leaseflow.dto.response.PropertyResponse;
import com.leaseflow.dto.response.UnitResponse;
import com.leaseflow.entity.Property;
import com.leaseflow.entity.Unit;
import com.leaseflow.entity.User;
import com.leaseflow.enums.Role;
import com.leaseflow.enums.UnitStatus;
import com.leaseflow.security.UserPrincipal;
import com.leaseflow.service.PropertyService;
import com.leaseflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Properties", description = "Property and Unit management endpoints")
@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PropertyController {

    private final PropertyService propertyService;
    private final UserService userService;

    @Operation(summary = "Create property with initial units")
    @PostMapping
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PropertyResponse>> createProperty(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PropertyRequest request) {

        User landlord = userService.findById(principal.getId());

        Property property = Property.builder()
                .name(request.getName())
                .address(request.getAddress())
                .propertyType(request.getPropertyType())
                .landlord(landlord)
                .build();

        List<Unit> units = request.getUnits().stream()
                .map(u -> Unit.builder()
                        .unitNumber(u.getUnitNumber())
                        .floor(u.getFloor())
                        .status(UnitStatus.VACANT)
                        .build())
                .toList();

        Property created = propertyService.createProperty(property, units);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Property created successfully.", PropertyResponse.fromWithUnits(created)));
    }

    @Operation(summary = "Get landlord's properties (paginated)")
    @GetMapping
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<PropertyResponse>>> getProperties(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        User user = userService.findById(principal.getId());
        Page<Property> page = (user.getRole() == Role.ROLE_ADMIN)
                ? propertyService.findAll(pageable)
                : propertyService.findByLandlord(principal.getId(), pageable);

        Page<PropertyResponse> dtoPage = page.map(PropertyResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(dtoPage)));
    }

    @Operation(summary = "Get units for a specific property owned by the landlord")
    @GetMapping("/{id}/units")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UnitResponse>>> getPropertyUnits(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        User user = userService.findById(principal.getId());
        List<Unit> units = (user.getRole() == Role.ROLE_ADMIN)
                ? propertyService.findUnitsByPropertyAdmin(id)
                : propertyService.findUnitsByProperty(id, principal.getId());

        List<UnitResponse> dtos = units.stream().map(UnitResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }
}
