package com.leaseflow.controller;

import com.leaseflow.dto.common.ApiResponse;
import com.leaseflow.dto.request.BillingEstimateRequest;
import com.leaseflow.dto.response.BillingEstimateResponse;
import com.leaseflow.pattern.strategy.BillingStrategy;
import com.leaseflow.pattern.strategy.BillingStrategyFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "Billing", description = "Utility charge estimation via Strategy pattern")
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BillingController {

    private final BillingStrategyFactory billingStrategyFactory;

    @Operation(summary = "Calculate an estimated utility charge using the Strategy Pattern")
    @PostMapping("/estimate")
    public ResponseEntity<ApiResponse<BillingEstimateResponse>> estimateCharge(
            @Valid @RequestBody BillingEstimateRequest request) {

        BillingStrategy strategy = billingStrategyFactory.getStrategy(
                request.getPropertyType(), request.getUtilityType());

        BigDecimal charge = strategy.calculateUtilityCharge(request.getUnits());

        BillingEstimateResponse response = BillingEstimateResponse.builder()
                .propertyType(request.getPropertyType())
                .utilityType(request.getUtilityType())
                .unitsConsumed(request.getUnits())
                .estimatedCharge(charge)
                .strategyDescription(strategy.getDescription())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

