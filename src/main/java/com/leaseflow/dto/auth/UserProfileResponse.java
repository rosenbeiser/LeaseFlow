package com.leaseflow.dto.auth;

import com.leaseflow.enums.Role;
import lombok.Builder;
import lombok.Getter;

/** Payload for {@code GET /api/v1/auth/me}. */
@Getter
@Builder
public class UserProfileResponse {

    private final Long   id;
    private final String email;
    private final String fullName;
    private final String phone;
    private final Role   role;
}

