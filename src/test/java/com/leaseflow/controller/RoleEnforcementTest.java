package com.leaseflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaseflow.dto.request.MeterReadingRequest;
import com.leaseflow.entity.User;
import com.leaseflow.enums.Role;
import com.leaseflow.enums.UtilityType;
import com.leaseflow.repository.UserRepository;
import com.leaseflow.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Role Enforcement and Security Filters Tests")
class RoleEnforcementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String tenantToken;

    @BeforeEach
    void setUp() {
        User tenant = userRepository.save(User.builder()
                .fullName("Tenant Security Test")
                .email("tenant.sec@leaseflow.dev")
                .password(passwordEncoder.encode("Pass@123"))
                .role(Role.ROLE_TENANT)
                .build());

        tenantToken = jwtTokenProvider.generateToken(tenant.getId(), tenant.getEmail(), tenant.getRole().name());
    }

    @Test
    @DisplayName("Unauthenticated request to POST /api/v1/meter-readings returns 401 Unauthorized")
    void testNoTokenReturns401() throws Exception {
        MeterReadingRequest req = new MeterReadingRequest();
        req.setUnitId(1L);
        req.setUtilityType(UtilityType.ELECTRICITY);
        req.setCurrentReading(new BigDecimal("150.00"));
        req.setReadingDate(LocalDate.now());

        mockMvc.perform(post("/api/v1/meter-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("TENANT token on POST /api/v1/meter-readings returns 403 Forbidden")
    void testTenantTokenReturns403() throws Exception {
        MeterReadingRequest req = new MeterReadingRequest();
        req.setUnitId(1L);
        req.setUtilityType(UtilityType.ELECTRICITY);
        req.setCurrentReading(new BigDecimal("150.00"));
        req.setReadingDate(LocalDate.now());

        mockMvc.perform(post("/api/v1/meter-readings")
                        .header("Authorization", "Bearer " + tenantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}

