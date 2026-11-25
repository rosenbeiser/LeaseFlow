package com.leaseflow.dto.auth;

import com.leaseflow.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for {@code POST /api/v1/auth/register}.
 * Only {@code ROLE_LANDLORD} and {@code ROLE_TENANT} are self-registrable;
 * {@code ROLE_ADMIN} is rejected at the service layer.
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @Size(max = 30)
    private String phone;

    @NotNull(message = "Role is required")
    private Role role;
}

