package com.leaseflow.controller;

import com.leaseflow.dto.common.ApiResponse;
import com.leaseflow.dto.response.InvoiceResponse;
import com.leaseflow.dto.response.LeaseResponse;
import com.leaseflow.dto.response.PaymentResponse;
import com.leaseflow.entity.Invoice;
import com.leaseflow.entity.Lease;
import com.leaseflow.entity.Payment;
import com.leaseflow.security.UserPrincipal;
import com.leaseflow.service.DashboardService;
import com.leaseflow.service.LeaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Dashboard", description = "Role-specific analytical KPI and overview endpoints")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;
    private final LeaseService leaseService;

    @Operation(summary = "Get landlord dashboard KPIs and charts")
    @GetMapping("/landlord")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLandlordDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {

        Map<String, Object> rawData = dashboardService.getLandlordDashboard(principal.getId());

        // Transform recent invoices to DTOs so entity fields aren't raw exposed
        Map<String, Object> responseData = new LinkedHashMap<>(rawData);
        if (responseData.get("recentInvoices") instanceof List<?> list) {
            List<InvoiceResponse> dtoList = list.stream()
                    .filter(item -> item instanceof Invoice)
                    .map(item -> InvoiceResponse.from((Invoice) item))
                    .toList();
            responseData.put("recentInvoices", dtoList);
        }

        return ResponseEntity.ok(ApiResponse.ok(responseData));
    }

    @Operation(summary = "Get tenant dashboard summary and next due bill")
    @GetMapping("/tenant")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTenantDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {

        Map<String, Object> rawData = dashboardService.getTenantDashboard(principal.getId());
        Map<String, Object> responseData = new LinkedHashMap<>(rawData);

        // Convert currentLease to LeaseResponse if present
        if (responseData.get("currentLease") instanceof Lease lease) {
            responseData.put("currentLease", LeaseResponse.from(lease, leaseService.getAllowedActions(lease)));
        }

        // Convert nextDueInvoice to InvoiceResponse if present
        if (responseData.get("nextDueInvoice") instanceof Invoice invoice) {
            responseData.put("nextDueInvoice", InvoiceResponse.from(invoice));
        }

        // Convert recentPayments to PaymentResponse if present
        if (responseData.get("recentPayments") instanceof List<?> payments) {
            List<PaymentResponse> paymentDtos = payments.stream()
                    .filter(p -> p instanceof Payment)
                    .map(p -> PaymentResponse.from((Payment) p))
                    .toList();
            responseData.put("recentPayments", paymentDtos);
        }

        return ResponseEntity.ok(ApiResponse.ok(responseData));
    }
}

