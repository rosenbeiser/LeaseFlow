package com.leaseflow.controller;

import com.leaseflow.entity.User;
import com.leaseflow.enums.Role;
import com.leaseflow.repository.UserRepository;
import com.leaseflow.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Dashboard Controller RBAC Tests")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String landlordToken;
    private String tenantToken;

    @BeforeEach
    void setUp() {
        User landlord = userRepository.save(User.builder()
                .fullName("Landlord User")
                .email("dash.landlord@leaseflow.dev")
                .password(passwordEncoder.encode("Pass@123"))
                .role(Role.ROLE_LANDLORD)
                .build());

        User tenant = userRepository.save(User.builder()
                .fullName("Tenant User")
                .email("dash.tenant@leaseflow.dev")
                .password(passwordEncoder.encode("Pass@123"))
                .role(Role.ROLE_TENANT)
                .build());

        landlordToken = jwtTokenProvider.generateToken(landlord.getId(), landlord.getEmail(), landlord.getRole().name());
        tenantToken   = jwtTokenProvider.generateToken(tenant.getId(), tenant.getEmail(), tenant.getRole().name());
    }

    @Test
    @DisplayName("Landlord accesses landlord dashboard successfully; Tenant gets 403 Forbidden")
    void testLandlordDashboardRoleGuard() throws Exception {
        // Landlord accesses landlord dashboard -> 200 OK
        mockMvc.perform(get("/api/v1/dashboard/landlord")
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.propertyCount").exists())
                .andExpect(jsonPath("$.data.totalUnits").exists());

        // Tenant tries to access landlord dashboard -> 403 Forbidden
        mockMvc.perform(get("/api/v1/dashboard/landlord")
                        .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Tenant accesses tenant dashboard successfully; Landlord gets 403 Forbidden")
    void testTenantDashboardRoleGuard() throws Exception {
        // Tenant accesses tenant dashboard -> 200 OK
        mockMvc.perform(get("/api/v1/dashboard/tenant")
                        .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Landlord tries to access tenant dashboard -> 403 Forbidden
        mockMvc.perform(get("/api/v1/dashboard/tenant")
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}

