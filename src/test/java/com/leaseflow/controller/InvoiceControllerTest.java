package com.leaseflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaseflow.entity.*;
import com.leaseflow.enums.*;
import com.leaseflow.repository.*;
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
import java.time.YearMonth;
import java.util.ArrayList;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Invoice Controller Integration Tests")
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private LeaseRepository leaseRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User landlord;
    private User tenantA;
    private User tenantB;
    private Lease leaseA;
    private Lease leaseB;

    private String landlordToken;
    private String tenantAToken;
    private String tenantBToken;

    @BeforeEach
    void setUp() {
        landlord = userRepository.save(User.builder()
                .fullName("Landlord One")
                .email("landlord.test@leaseflow.dev")
                .password(passwordEncoder.encode("Password@123"))
                .role(Role.ROLE_LANDLORD)
                .build());

        tenantA = userRepository.save(User.builder()
                .fullName("Tenant A")
                .email("tenanta.test@leaseflow.dev")
                .password(passwordEncoder.encode("Password@123"))
                .role(Role.ROLE_TENANT)
                .build());

        tenantB = userRepository.save(User.builder()
                .fullName("Tenant B")
                .email("tenantb.test@leaseflow.dev")
                .password(passwordEncoder.encode("Password@123"))
                .role(Role.ROLE_TENANT)
                .build());

        Property property = propertyRepository.save(Property.builder()
                .name("Test Complex")
                .address("100 Main St")
                .propertyType(PropertyType.RESIDENTIAL)
                .landlord(landlord)
                .build());

        Unit unitA = unitRepository.save(Unit.builder()
                .unitNumber("A-101")
                .property(property)
                .status(UnitStatus.OCCUPIED)
                .build());

        Unit unitB = unitRepository.save(Unit.builder()
                .unitNumber("B-102")
                .property(property)
                .status(UnitStatus.OCCUPIED)
                .build());

        leaseA = leaseRepository.save(Lease.builder()
                .unit(unitA)
                .tenant(tenantA)
                .startDate(LocalDate.now().minusMonths(2))
                .endDate(LocalDate.now().plusMonths(10))
                .baseRent(new BigDecimal("1200.00"))
                .securityDeposit(new BigDecimal("2400.00"))
                .status(LeaseStatus.ACTIVE)
                .statusHistory(new ArrayList<>())
                .build());

        leaseB = leaseRepository.save(Lease.builder()
                .unit(unitB)
                .tenant(tenantB)
                .startDate(LocalDate.now().minusMonths(2))
                .endDate(LocalDate.now().plusMonths(10))
                .baseRent(new BigDecimal("1400.00"))
                .securityDeposit(new BigDecimal("2800.00"))
                .status(LeaseStatus.ACTIVE)
                .statusHistory(new ArrayList<>())
                .build());

        landlordToken = jwtTokenProvider.generateToken(landlord.getId(), landlord.getEmail(), landlord.getRole().name());
        tenantAToken  = jwtTokenProvider.generateToken(tenantA.getId(), tenantA.getEmail(), tenantA.getRole().name());
        tenantBToken  = jwtTokenProvider.generateToken(tenantB.getId(), tenantB.getEmail(), tenantB.getRole().name());
    }

    @Test
    @DisplayName("Landlord generates an invoice for an active lease; duplicate month generation returns 409")
    void testLandlordGeneratesInvoiceAndRejectsDuplicate() throws Exception {
        String month = YearMonth.now().toString();

        // 1. Generate invoice
        mockMvc.perform(post("/api/v1/invoices/generate/" + leaseA.getId() + "?month=" + month)
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.baseRentAmount").value(1200.00))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        // 2. Duplicate generation attempt for the same lease and month returns 409 CONFLICT
        mockMvc.perform(post("/api/v1/invoices/generate/" + leaseA.getId() + "?month=" + month)
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("Tenant can read own bills, but cannot read another tenant's invoice (403 Forbidden)")
    void testTenantBillsDataIsolation() throws Exception {
        String month = YearMonth.now().toString();

        // Generate invoice for Tenant B's lease
        mockMvc.perform(post("/api/v1/invoices/generate/" + leaseB.getId() + "?month=" + month)
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isCreated());

        Invoice invoiceB = invoiceRepository.findByLeaseId(leaseB.getId()).get(0);

        // Tenant A views own bills -> empty list
        mockMvc.perform(get("/api/v1/invoices/my-bills")
                        .header("Authorization", "Bearer " + tenantAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        // Tenant B views own bills -> sees 1 bill
        mockMvc.perform(get("/api/v1/invoices/my-bills")
                        .header("Authorization", "Bearer " + tenantBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(invoiceB.getId()));

        // Tenant A tries to directly access Tenant B's invoice -> 403 Forbidden
        mockMvc.perform(get("/api/v1/invoices/" + invoiceB.getId())
                        .header("Authorization", "Bearer " + tenantAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}

