package com.leaseflow.controller;

import com.leaseflow.dto.response.ApiResponse;
import com.leaseflow.security.UserPrincipal;
import com.leaseflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user profile management.
 *
 * <p>Exposes endpoints for authenticated users to view and update their own
 * profile information. Users can only access and modify their own profile
 * (admin can view any profile via the admin endpoints).
 *
 * @author Student 2207084
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "User profile management")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get current user profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Profile request for user id={}", principal.getId());
        var user = userService.findById(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping
    @Operation(summary = "Update current user profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        log.debug("Profile update for user id={}", principal.getId());
        var updated = userService.updateProfile(principal.getId(),
                request.getFullName(), request.getPhone());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/password")
    @Operation(summary = "Change current user password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.debug("Password change for user id={}", principal.getId());
        userService.changePassword(principal.getId(),
                request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully"));
    }

    // ── Inner request DTOs ────────────────────────────────────────────────────

    @Data
    public static class UpdateProfileRequest {
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        private String fullName;

        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        private String phone;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "New password must be at least 8 characters")
        private String newPassword;
    }
}
