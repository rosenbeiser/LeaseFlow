package com.leaseflow.controller;

import com.leaseflow.dto.common.ApiResponse;
import com.leaseflow.dto.common.PageResponse;
import com.leaseflow.dto.request.LeaseRequest;
import com.leaseflow.dto.request.SettlementRequest;
import com.leaseflow.dto.response.LeaseResponse;
import com.leaseflow.dto.response.SettlementResponse;
import com.leaseflow.entity.Lease;
import com.leaseflow.entity.Unit;
import com.leaseflow.entity.User;
import com.leaseflow.enums.LeaseStatus;
import com.leaseflow.enums.Role;
import com.leaseflow.exception.AccessDeniedException;
import com.leaseflow.security.UserPrincipal;
import com.leaseflow.service.InvoiceService;
import com.leaseflow.service.LeaseService;
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

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Leases", description = "Lease lifecycle state machine management endpoints")
@RestController
@RequestMapping("/api/v1/leases")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class LeaseController {

    private final LeaseService leaseService;
    private final PropertyService propertyService;
    private final UserService userService;
    private final InvoiceService invoiceService;

    @Operation(summary = "Create a draft lease")
    @PostMapping
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeaseResponse>> createLease(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LeaseRequest request) {

        Unit unit = principal.getRole().equals("ROLE_ADMIN")
                ? propertyService.findUnitById(request.getUnitId())
                : propertyService.findUnitByIdAndLandlord(request.getUnitId(), principal.getId());
        User tenant = userService.findById(request.getTenantId());

        Lease lease = Lease.builder()
                .unit(unit)
                .tenant(tenant)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .baseRent(request.getBaseRent())
                .securityDeposit(request.getSecurityDeposit())
                .status(LeaseStatus.DRAFT)
                .build();

        Lease created = leaseService.createLease(lease);
        List<String> allowedActions = leaseService.getAllowedActions(created);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Lease created successfully.", LeaseResponse.from(created, allowedActions)));
    }

    @Operation(summary = "Get landlord's leases (filtered by status, paginated)")
    @GetMapping
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<LeaseResponse>>> getLandlordLeases(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) LeaseStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        User user = userService.findById(principal.getId());
        Page<Lease> page = (user.getRole() == Role.ROLE_ADMIN)
                ? leaseService.findAllWithFilter(status, pageable)
                : leaseService.findByLandlord(principal.getId(), status, pageable);
        Page<LeaseResponse> dtoPage = page.map(l -> LeaseResponse.from(l, leaseService.getAllowedActions(l)));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(dtoPage)));
    }

    @Operation(summary = "Get tenant's own leases")
    @GetMapping("/my-leases")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<ApiResponse<PageResponse<LeaseResponse>>> getMyLeases(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        Page<Lease> page = leaseService.findByTenant(principal.getId(), pageable);
        Page<LeaseResponse> dtoPage = page.map(l -> LeaseResponse.from(l, leaseService.getAllowedActions(l)));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(dtoPage)));
    }

    @Operation(summary = "Get lease details with timeline and allowed actions")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeaseResponse>> getLeaseById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        Lease lease = leaseService.findById(id);

        boolean isLandlordOwner = lease.getUnit().getProperty().getLandlord().getId().equals(principal.getId());
        boolean isTenantOwner = lease.getTenant().getId().equals(principal.getId());
        boolean isAdmin = principal.getRole().equals("ROLE_ADMIN");

        if (!isLandlordOwner && !isTenantOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to view this lease.");
        }

        List<String> allowedActions = leaseService.getAllowedActions(lease);
        return ResponseEntity.ok(ApiResponse.ok(LeaseResponse.from(lease, allowedActions)));
    }

    @Operation(summary = "Activate a draft lease (DRAFT -> ACTIVE)")
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeaseResponse>> activateLease(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        if (!principal.getRole().equals("ROLE_ADMIN")) {
            leaseService.findByIdAndLandlord(id, principal.getId());
        }
        User actor = userService.findById(principal.getId());
        Lease activated = leaseService.activate(id, actor);
        List<String> allowedActions = leaseService.getAllowedActions(activated);
        return ResponseEntity.ok(ApiResponse.ok("Lease activated successfully.", LeaseResponse.from(activated, allowedActions)));
    }

    @Operation(summary = "Serve notice on an active lease (ACTIVE -> NOTICE_SERVED)")
    @PutMapping("/{id}/notice")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeaseResponse>> serveNotice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        if (!principal.getRole().equals("ROLE_ADMIN")) {
            leaseService.findByIdAndLandlord(id, principal.getId());
        }
        User actor = userService.findById(principal.getId());
        Lease updated = leaseService.serveNotice(id, actor);
        List<String> allowedActions = leaseService.getAllowedActions(updated);
        return ResponseEntity.ok(ApiResponse.ok("Notice served successfully.", LeaseResponse.from(updated, allowedActions)));
    }

    @Operation(summary = "Terminate a lease (ACTIVE/NOTICE_SERVED -> TERMINATED)")
    @PutMapping("/{id}/terminate")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeaseResponse>> terminateLease(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        if (!principal.getRole().equals("ROLE_ADMIN")) {
            leaseService.findByIdAndLandlord(id, principal.getId());
        }
        User actor = userService.findById(principal.getId());
        Lease terminated = leaseService.terminate(id, actor);
        List<String> allowedActions = leaseService.getAllowedActions(terminated);
        return ResponseEntity.ok(ApiResponse.ok("Lease terminated successfully.", LeaseResponse.from(terminated, allowedActions)));
    }

    @Operation(summary = "Settle a terminated lease (TERMINATED -> SETTLED)")
    @PostMapping("/{id}/settle")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SettlementResponse>> settleLease(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody SettlementRequest request) {

        Lease lease = principal.getRole().equals("ROLE_ADMIN")
                ? leaseService.findById(id)
                : leaseService.findByIdAndLandlord(id, principal.getId());
        User actor = userService.findById(principal.getId());

        BigDecimal refund = invoiceService.calculateRefund(id, request.getDamagesAmount());
        leaseService.settle(id, actor, request.getDamagesAmount(), request.getNotes());

        BigDecimal unpaidBal = lease.getInvoices().stream()
                .filter(i -> i.getStatus() != com.leaseflow.enums.InvoiceStatus.PAID)
                .map(i -> i.getTotalPayable().subtract(i.getAmountPaid()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SettlementResponse resp = SettlementResponse.builder()
                .leaseId(id)
                .securityDeposit(lease.getSecurityDeposit())
                .unpaidInvoiceBalance(unpaidBal)
                .damagesAmount(request.getDamagesAmount())
                .refundAmount(refund.abs())
                .tenantOwes(refund.compareTo(BigDecimal.ZERO) < 0)
                .notes(request.getNotes())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Lease settled successfully.", resp));
    }
}
