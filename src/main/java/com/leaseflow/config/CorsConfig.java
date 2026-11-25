package com.leaseflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration for the LeaseFlow API.
 *
 * <p>Restricts cross-origin requests to the configured frontend origin, only
 * allows the HTTP methods and headers needed by the application, and exposes
 * the Authorization header so the browser can read JWT tokens from XHR
 * responses.
 *
 * <p>The allowed origin is supplied via the {@code app.cors.allowed-origin}
 * property (defaults to {@code http://localhost:8888}) so deployments only
 * need to change their environment variable — no code change required.
 *
 * @author Student 2207082
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origin:http://localhost:8888}")
    private String allowedOrigin;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Trust only the declared frontend origin (never wildcard in production)
        config.setAllowedOrigins(List.of(allowedOrigin));

        // HTTP methods used by REST clients and the SPA
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow the Authorization header so Bearer tokens can be sent
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));

        // Expose Authorization so XHR response headers are readable in the browser
        config.setExposedHeaders(List.of("Authorization"));

        // Allow credentials (cookies / auth headers) for same-site requests
        config.setAllowCredentials(true);

        // Cache pre-flight result for 1 hour to reduce OPTIONS round-trips
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
