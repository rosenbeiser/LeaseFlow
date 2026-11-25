package com.leaseflow.controller;

import com.leaseflow.dto.common.ApiResponse;
import com.leaseflow.dto.common.PageResponse;
import com.leaseflow.dto.response.PaymentResponse;
import com.leaseflow.entity.Payment;
import com.leaseflow.security.UserPrincipal;
import com.leaseflow.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payments", description = "Tenant payment transaction history")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Get tenant's payment history (paginated)")
    @GetMapping("/my")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getMyPayments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "paidAt") Pageable pageable) {

        Page<Payment> page = paymentService.findByTenant(principal.getId(), pageable);
        Page<PaymentResponse> dtoPage = page.map(PaymentResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(dtoPage)));
    }
}

