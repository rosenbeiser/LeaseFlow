package com.leaseflow.service;

import com.leaseflow.dto.auth.*;
import com.leaseflow.entity.User;
import com.leaseflow.enums.Role;
import com.leaseflow.exception.BusinessRuleException;
import com.leaseflow.exception.DuplicateResourceException;
import com.leaseflow.repository.UserRepository;
import com.leaseflow.security.JwtTokenProvider;
import com.leaseflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtTokenProvider      jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    // ── Register ──────────────────────────────────────────────────────────────

    /**
     * Registers a new user. {@code ROLE_ADMIN} registration is rejected — admins
     * are seeded and cannot be self-created (this is a business security rule, not
     * just a UI restriction).
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() == Role.ROLE_ADMIN) {
            throw new BusinessRuleException(
                    "Admin accounts cannot be registered through this endpoint.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "An account with email '" + request.getEmail() + "' already exists.");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .build();

        user = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getEmail(), user.getRole().name());

        return buildAuthResponse(token, user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Authenticates credentials and returns a JWT. Returns 401 on bad credentials
     * via the {@link com.leaseflow.security.JwtAuthenticationEntryPoint} when
     * {@link BadCredentialsException} propagates.
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(
                principal.getId(), principal.getEmail(), principal.getRole());

        User user = userRepository.findByEmail(principal.getEmail())
                .orElseThrow();

        return buildAuthResponse(token, user);
    }

    // ── Me ────────────────────────────────────────────────────────────────────

    public UserProfileResponse getProfile(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow();
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .build();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}
