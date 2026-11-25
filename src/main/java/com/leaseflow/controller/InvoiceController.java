package com.leaseflow.controller;

import com.leaseflow.dto.common.ApiResponse;
import com.leaseflow.dto.common.PageResponse;
import com.leaseflow.dto.request.PaymentRequest;
import com.leaseflow.dto.response.InvoiceResponse;
import com.leaseflow.dto.response.PaymentResponse;
import com.leaseflow.entity.Invoice;
import com.leaseflow.entity.Payment;
import com.leaseflow.entity.User;
import com.leaseflow.enums.InvoiceStatus;
import com.leaseflow.exception.AccessDeniedException;
import com.leaseflow.security.UserPrincipal;
import com.leaseflow.service.InvoiceService;
import com.leaseflow.service.LeaseService;
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

import java.time.YearMonth;
import java.util.List;

@Tag(name = "Invoices", description = "Invoice generation, payment, and billing breakdown")
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final LeaseService leaseService;
    private final UserService userService;

    @Operation(summary = "Generate a monthly invoice for a lease")
    @PostMapping("/generate/{leaseId}")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> generateInvoice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long leaseId,
            @RequestParam String month) {

        if (!principal.getRole().equals("ROLE_ADMIN")) {
            leaseService.findByIdAndLandlord(leaseId, principal.getId());
        }
        User actor = userService.findById(principal.getId());

        YearMonth billingMonth = YearMonth.parse(month.trim());
        Invoice invoice = invoiceService.generateInvoice(leaseId, billingMonth, actor);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Invoice generated successfully.", InvoiceResponse.from(invoice)));
    }

    @Operation(summary = "Get landlord's invoices (filtered by status and month, paginated)")
    @GetMapping
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<InvoiceResponse>>> getLandlordInvoices(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String month,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        Integer monthInt = (month != null && !month.isBlank())
                ? InvoiceService.toMonthInt(YearMonth.parse(month.trim()))
                : null;

        User user = userService.findById(principal.getId());
        Page<Invoice> page = (user.getRole() == com.leaseflow.enums.Role.ROLE_ADMIN)
                ? invoiceService.findAllPlatform(status, monthInt, pageable)
                : invoiceService.findByLandlord(principal.getId(), status, monthInt, pageable);
        Page<InvoiceResponse> dtoPage = page.map(InvoiceResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(dtoPage)));
    }

    @Operation(summary = "Get all invoices for a specific lease")
    @GetMapping("/lease/{leaseId}")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoicesByLease(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long leaseId) {

        if (!principal.getRole().equals("ROLE_ADMIN")) {
            leaseService.findByIdAndLandlord(leaseId, principal.getId());
        }

        List<Invoice> invoices = invoiceService.findByLeaseId(leaseId);
        List<InvoiceResponse> dtos = invoices.stream().map(InvoiceResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @Operation(summary = "Get tenant's own invoices (filtered by status, paginated)")
    @GetMapping("/my-bills")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<ApiResponse<PageResponse<InvoiceResponse>>> getMyBills(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) InvoiceStatus status,
            @PageableDefault(size = 10, sort = "dueDate") Pageable pageable) {

        Page<Invoice> page = invoiceService.findByTenant(principal.getId(), status, pageable);
        Page<InvoiceResponse> dtoPage = page.map(InvoiceResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(dtoPage)));
    }

    @Operation(summary = "Get invoice details with itemized line items and payment history")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        Invoice invoice = invoiceService.findById(id);

        boolean isLandlordOwner = invoice.getLease().getUnit().getProperty().getLandlord().getId().equals(principal.getId());
        boolean isTenantOwner = invoice.getLease().getTenant().getId().equals(principal.getId());
        boolean isAdmin = principal.getRole().equals("ROLE_ADMIN");

        if (!isLandlordOwner && !isTenantOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to view this invoice.");
        }

        return ResponseEntity.ok(ApiResponse.ok(InvoiceResponse.from(invoice)));
    }

    @Operation(summary = "Pay an invoice (partial or full)")
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<ApiResponse<PaymentResponse>> payInvoice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request) {

        Payment payment = invoiceService.recordPayment(
                id,
                request.getAmount(),
                request.getPaymentMethod().name(),
                request.getTransactionReference(),
                principal.getId());

        return ResponseEntity.ok(ApiResponse.ok("Payment recorded successfully.", PaymentResponse.from(payment)));
    }
}

