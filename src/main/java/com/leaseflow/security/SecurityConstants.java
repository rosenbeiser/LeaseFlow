package com.leaseflow.security;

/**
 * Central registry of security-related constants used across the application.
 *
 * <p>Keeping constants in one place prevents magic strings from being scattered
 * throughout filters, controllers, and tests, making future changes (e.g. a new
 * public endpoint or a different token prefix) a one-line update.
 *
 * @author Student 2207082
 */
public final class SecurityConstants {

    private SecurityConstants() {} // utility class — no instances

    /** Bearer token prefix expected/sent in the Authorization header. */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** HTTP header name carrying the JWT token. */
    public static final String AUTH_HEADER = "Authorization";

    /** Minimum length (characters) required for the JWT signing secret. */
    public static final int MIN_SECRET_LENGTH = 32;

    /** Default token expiry in milliseconds (24 hours). */
    public static final long DEFAULT_EXPIRY_MS = 86_400_000L;

    // ── Public (unauthenticated) endpoint patterns ────────────────────────────

    /** Authentication endpoints — login and registration. */
    public static final String[] AUTH_ENDPOINTS = {
            "/api/v1/auth/**"
    };

    /** Static front-end assets served by Spring Boot. */
    public static final String[] STATIC_ASSETS = {
            "/",
            "/index.html",
            "/app.html",
            "/css/**",
            "/js/**",
            "/favicon.ico"
    };

    /** SpringDoc / Swagger UI endpoints for API documentation. */
    public static final String[] SWAGGER_ENDPOINTS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    /** Health and actuator endpoints. */
    public static final String[] ACTUATOR_ENDPOINTS = {
            "/actuator/health"
    };
}
