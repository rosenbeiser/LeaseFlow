package com.leaseflow.config;

import com.leaseflow.security.JwtAccessDeniedHandler;
import com.leaseflow.security.JwtAuthenticationEntryPoint;
import com.leaseflow.security.JwtAuthenticationFilter;
import com.leaseflow.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central Spring Security configuration.
 *
 * <ul>
 *   <li>Stateless JWT — no sessions, no CSRF cookie.</li>
 *   <li>{@code @EnableMethodSecurity} enables {@code @PreAuthorize} on service/controller methods.</li>
 *   <li>Public paths: static assets, Actuator health, Swagger UI, auth endpoints.</li>
 *   <li>Security headers: CSP, X-Content-Type-Options, X-Frame-Options, Referrer-Policy.</li>
 *   <li>CORS origins configured via {@code CORS_ALLOWED_ORIGINS} env var (default: same-origin).</li>
 * </ul>
 *
 * <p><b>Security principle:</b> the frontend hides unauthorized UI for usability, but
 * the backend is the sole authority. Every protected endpoint verifies the JWT and role
 * regardless of what the client sends.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Value("${app.cors.allowed-origins:}")
    private String corsAllowedOrigins;

    // ── Password Encoder ──────────────────────────────────────────────────────

    /**
     * BCrypt with cost factor 12. Deliberately high to resist brute-force attacks.
     * Factor 12 adds ~300 ms per hash — acceptable for authentication, strong against offline attacks.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ── Authentication ────────────────────────────────────────────────────────

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // ── Security Filter Chain ─────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── CSRF: disabled — stateless JWT does not use sessions ──────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS ──────────────────────────────────────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── Session: stateless ────────────────────────────────────────────
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Exception handlers ────────────────────────────────────────────
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(authEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))

            // ── Authorization rules ───────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                    // Public static assets
                    .requestMatchers("/", "/index.html", "/app.html").permitAll()
                    .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
                    // Actuator health
                    .requestMatchers("/actuator/health").permitAll()
                    // Swagger / OpenAPI
                    .requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                                     "/api-docs", "/api-docs/**", "/v3/api-docs/**").permitAll()
                    // Auth endpoints
                    .requestMatchers(HttpMethod.POST,
                            "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                    // Everything else under /api/v1/** requires authentication
                    .requestMatchers("/api/v1/**").authenticated()
                    // Any other request (e.g. additional static files)
                    .anyRequest().permitAll()
            )

            // ── Security headers ──────────────────────────────────────────────
            .headers(headers -> headers
                    .contentTypeOptions(cto -> {})   // X-Content-Type-Options: nosniff
                    .frameOptions(fo -> fo.deny())   // X-Frame-Options: DENY
                    .referrerPolicy(rp -> rp.policy(
                            ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .contentSecurityPolicy(csp -> csp.policyDirectives(buildCsp()))
            )

            // ── Authentication provider + JWT filter ──────────────────────────
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ── CORS Configuration ────────────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        if (corsAllowedOrigins != null && !corsAllowedOrigins.isBlank()) {
            List<String> origins = List.of(corsAllowedOrigins.split(","));
            config.setAllowedOrigins(origins);
        } else {
            // Default: same-origin only (empty list in Spring means block all cross-origin)
            config.setAllowedOriginPatterns(List.of());
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    // ── Content Security Policy ───────────────────────────────────────────────

    /**
     * CSP allows self plus pinned CDN hosts for Bootstrap 5.3, Chart.js 4, Bootstrap Icons,
     * and Google Fonts. Inline styles are needed for Bootstrap's dynamic utility classes.
     */
    private String buildCsp() {
        return String.join("; ",
                "default-src 'self'",
                "script-src 'self' https://cdn.jsdelivr.net",
                "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com",
                "font-src 'self' https://fonts.gstatic.com",
                "img-src 'self' data:",
                "connect-src 'self'",
                "frame-ancestors 'none'",
                "base-uri 'self'",
                "form-action 'self'"
        );
    }
}

