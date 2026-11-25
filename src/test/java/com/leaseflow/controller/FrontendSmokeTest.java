package com.leaseflow.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Frontend & Static Resources Smoke Tests")
class FrontendSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public static HTML landing and app shell are served without credentials (200 OK)")
    void testPublicHtmlPagesServed() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/app.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Static assets CSS and JS are served with appropriate security headers")
    void testStaticAssetsAndSecurityHeaders() throws Exception {
        mockMvc.perform(get("/css/theme.css"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")));

        mockMvc.perform(get("/js/api.js"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Protected API endpoint /api/v1/leases rejects unauthenticated requests with 401 Unauthorized")
    void testProtectedApiRejectsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/leases"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }
}

