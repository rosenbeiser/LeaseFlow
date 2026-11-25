package com.leaseflow.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Stateless JWT provider using jjwt 0.12.x API.
 *
 * <p>Secret and expiry are resolved exclusively from environment variables
 * {@code JWT_SECRET} and {@code JWT_EXPIRATION_MS} (via {@code application.yml})
 * — they are never hardcoded. The secret must be at least 256 bits (32 chars) for HS256.
 *
 * <p>The token payload carries:
 * <ul>
 *   <li>{@code sub}  — user email (the Spring Security username)</li>
 *   <li>{@code id}   — user database id</li>
 *   <li>{@code role} — e.g. {@code ROLE_LANDLORD}</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // Derive a HMAC-SHA key from the configured secret bytes
        byte[] keyBytes = secret.length() >= 32
                ? secret.getBytes()
                : java.util.Arrays.copyOf(secret.getBytes(), 32);
        this.secretKey    = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /** Generate a signed JWT for the authenticated principal. */
    public String generateToken(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return generateToken(principal.getId(), principal.getEmail(), principal.getRole());
    }

    /** Generate a signed JWT directly from user fields (used in auth service). */
    public String generateToken(Long userId, String email, String role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("id",   userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /** Extract the email (subject) from a validated token. */
    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /** Extract the user id from a validated token. */
    public Long getUserIdFromToken(String token) {
        return parseClaims(token).get("id", Long.class);
    }

    /** Extract the role claim from a validated token. */
    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /** Returns {@code true} if the token is valid and not expired. */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.warn("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Malformed JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

