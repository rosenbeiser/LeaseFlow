package com.leaseflow.controller;

import com.leaseflow.dto.auth.UserProfileResponse;
import com.leaseflow.dto.common.ApiResponse;
import com.leaseflow.entity.User;
import com.leaseflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users", description = "User management and search endpoints")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Search or list tenants (for lease assignment)")
    @GetMapping("/tenants")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getTenants(@RequestParam(required = false) String email) {
        if (email != null && !email.isBlank()) {
            User tenant = userService.findTenantByEmail(email.trim());
            UserProfileResponse dto = UserProfileResponse.builder()
                    .id(tenant.getId())
                    .email(tenant.getEmail())
                    .fullName(tenant.getFullName())
                    .phone(tenant.getPhone())
                    .role(tenant.getRole())
                    .build();
            return ResponseEntity.ok(ApiResponse.ok(dto));
        }

        List<User> tenants = userService.findTenants();
        List<UserProfileResponse> dtos = tenants.stream()
                .map(t -> UserProfileResponse.builder()
                        .id(t.getId())
                        .email(t.getEmail())
                        .fullName(t.getFullName())
                        .phone(t.getPhone())
                        .role(t.getRole())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }
}

