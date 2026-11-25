package com.leaseflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaseflow.dto.auth.LoginRequest;
import com.leaseflow.dto.auth.RegisterRequest;
import com.leaseflow.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Authentication Controller Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Successfully registers a new tenant")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john.doe@test.com");
        request.setPassword("Secret@123");
        request.setPhone("1234567890");
        request.setRole(Role.ROLE_TENANT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.email").value("john.doe@test.com"))
                .andExpect(jsonPath("$.data.role").value("ROLE_TENANT"));
    }

    @Test
    @DisplayName("Rejects registration attempt with ROLE_ADMIN")
    void testRegisterAdminRejected() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Malicious Admin");
        request.setEmail("admin.attempt@test.com");
        request.setPassword("Secret@123");
        request.setRole(Role.ROLE_ADMIN);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Admin accounts cannot be registered")));
    }

    @Test
    @DisplayName("Rejects registration when email is already registered")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Duplicate User");
        request.setEmail("duplicate@test.com");
        request.setPassword("Secret@123");
        request.setRole(Role.ROLE_LANDLORD);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate attempt
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("Logs in with valid credentials and accesses /api/v1/auth/me")
    void testLoginAndGetProfile() throws Exception {
        // Register first
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setFullName("Alice Landlord");
        registerReq.setEmail("alice.landlord@test.com");
        registerReq.setPassword("Password@123");
        registerReq.setRole(Role.ROLE_LANDLORD);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Login
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("alice.landlord@test.com");
        loginReq.setPassword("Password@123");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).path("data").path("accessToken").asText();

        // Access /api/v1/auth/me
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("alice.landlord@test.com"))
                .andExpect(jsonPath("$.data.fullName").value("Alice Landlord"))
                .andExpect(jsonPath("$.data.role").value("ROLE_LANDLORD"));
    }

    @Test
    @DisplayName("Returns 401 Unauthorized on invalid login credentials")
    void testLoginBadCredentials() throws Exception {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("nonexistent@test.com");
        loginReq.setPassword("WrongPassword@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}

