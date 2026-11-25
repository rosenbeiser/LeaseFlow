package com.leaseflow.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc / OpenAPI 3 configuration.
 *
 * <p>Exposes Swagger UI at {@code /swagger-ui.html} with a "Authorize" button that
 * sends the Bearer JWT in the {@code Authorization} header for all protected endpoints.
 * No credentials are stored by Swagger — the token must be obtained from
 * {@code POST /api/v1/auth/login} first.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title       = "LeaseFlow API",
                version     = "1.0",
                description = "Smart Property Lease & Utility Billing Management System REST API",
                contact     = @Contact(name = "LeaseFlow Team"),
                license     = @License(name = "MIT")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local development"),
                @Server(url = "http://localhost:8080", description = "Docker Compose")
        }
)
@SecurityScheme(
        name        = "bearerAuth",
        type        = SecuritySchemeType.HTTP,
        scheme      = "bearer",
        bearerFormat = "JWT",
        description = "Obtain a token from POST /api/v1/auth/login then click Authorize and paste it here."
)
public class OpenApiConfig {
    // Configuration is entirely annotation-driven.
}

