package com.leaseflow.dto.auth;

import com.leaseflow.enums.Role;
import lombok.Builder;
import lombok.Getter;

/** Response payload for successful login and register. */
@Getter
@Builder
public class AuthResponse {

    private final String accessToken;
    private final String tokenType;
    private final long   expiresIn;

    // User info embedded so the frontend doesn't need a second /me call
    private final Long   userId;
    private final String email;
    private final String fullName;
    private final Role   role;
}

