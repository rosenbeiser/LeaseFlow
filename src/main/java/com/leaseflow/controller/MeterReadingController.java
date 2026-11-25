package com.leaseflow.controller;

import com.leaseflow.dto.common.ApiResponse;
import com.leaseflow.dto.common.PageResponse;
import com.leaseflow.dto.request.MeterReadingRequest;
import com.leaseflow.dto.response.MeterReadingResponse;
import com.leaseflow.entity.MeterReading;
import com.leaseflow.entity.Unit;
import com.leaseflow.security.UserPrincipal;
import com.leaseflow.service.MeterReadingService;
import com.leaseflow.service.PropertyService;
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

@Tag(name = "Meter Readings", description = "Utility meter readings recording and history")
@RestController
@RequestMapping("/api/v1/meter-readings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;
    private final PropertyService propertyService;

    @Operation(summary = "Record a utility meter reading")
    @PostMapping
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MeterReadingResponse>> recordReading(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MeterReadingRequest request) {

        Unit unit = principal.getRole().equals("ROLE_ADMIN")
                ? propertyService.findUnitById(request.getUnitId())
                : propertyService.findUnitByIdAndLandlord(request.getUnitId(), principal.getId());

        MeterReading mr = MeterReading.builder()
                .unit(unit)
                .utilityType(request.getUtilityType())
                .previousReading(request.getPreviousReading())
                .currentReading(request.getCurrentReading())
                .readingDate(request.getReadingDate())
                .recordedBy(principal.getId())
                .build();

        MeterReading saved = meterReadingService.record(mr, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Meter reading recorded successfully.", MeterReadingResponse.from(saved)));
    }

    @Operation(summary = "Get meter readings for a unit (paginated)")
    @GetMapping
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<MeterReadingResponse>>> getReadings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long unitId,
            @PageableDefault(size = 10, sort = "readingDate") Pageable pageable) {

        Page<MeterReading> page = meterReadingService.findByUnit(unitId, principal.getId(), pageable);
        Page<MeterReadingResponse> dtoPage = page.map(MeterReadingResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(dtoPage)));
    }
}
