package com.leaseflow.service;

import com.leaseflow.entity.*;
import com.leaseflow.enums.*;
import com.leaseflow.pattern.state.LeaseStateFactory;
import com.leaseflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Bootstraps demo data for the {@code dev} profile.
 * Credentials are printed to the startup log so developers can immediately log in.
 *
 * <p><b>Demo accounts:</b>
 * <pre>
 *   admin@leaseflow.dev      / Admin@123     (ADMIN)
 *   landlord1@leaseflow.dev  / Landlord@123  (LANDLORD)
 *   landlord2@leaseflow.dev  / Landlord@123  (LANDLORD)
 *   tenant1@leaseflow.dev    / Tenant@123    (TENANT)
 *   tenant2@leaseflow.dev    / Tenant@123    (TENANT)
 *   tenant3@leaseflow.dev    / Tenant@123    (TENANT)
 *   tenant4@leaseflow.dev    / Tenant@123    (TENANT)
 * </pre>
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final LeaseRepository leaseRepository;
    private final LeaseStatusHistoryRepository historyRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("DataSeeder: database already seeded, skipping.");
            return;
        }

        log.info("DataSeeder: seeding demo data...");

        // ── Users ────────────────────────────────────────────────────────────
        User admin = save(User.builder().email("admin@leaseflow.dev")
                .password(passwordEncoder.encode("Admin@123"))
                .fullName("System Admin").phone("0000000000").role(Role.ROLE_ADMIN).build());

        User landlord1 = save(User.builder().email("landlord1@leaseflow.dev")
                .password(passwordEncoder.encode("Landlord@123"))
                .fullName("Alice Landlord").phone("1111111111").role(Role.ROLE_LANDLORD).build());

        User landlord2 = save(User.builder().email("landlord2@leaseflow.dev")
                .password(passwordEncoder.encode("Landlord@123"))
                .fullName("Bob Landlord").phone("2222222222").role(Role.ROLE_LANDLORD).build());

        User tenant1 = save(User.builder().email("tenant1@leaseflow.dev")
                .password(passwordEncoder.encode("Tenant@123"))
                .fullName("Carol Tenant").phone("3333333333").role(Role.ROLE_TENANT).build());

        User tenant2 = save(User.builder().email("tenant2@leaseflow.dev")
                .password(passwordEncoder.encode("Tenant@123"))
                .fullName("Dave Tenant").phone("4444444444").role(Role.ROLE_TENANT).build());

        User tenant3 = save(User.builder().email("tenant3@leaseflow.dev")
                .password(passwordEncoder.encode("Tenant@123"))
                .fullName("Eve Tenant").phone("5555555555").role(Role.ROLE_TENANT).build());

        User tenant4 = save(User.builder().email("tenant4@leaseflow.dev")
                .password(passwordEncoder.encode("Tenant@123"))
                .fullName("Frank Tenant").phone("6666666666").role(Role.ROLE_TENANT).build());

        // ── Properties ────────────────────────────────────────────────────────
        Property prop1 = save(Property.builder()
                .name("Sunrise Apartments").address("12 Sunrise Ave, Springfield")
                .propertyType(PropertyType.RESIDENTIAL).landlord(landlord1).build());

        Property prop2 = save(Property.builder()
                .name("Downtown Plaza").address("88 Commerce St, Shelbyville")
                .propertyType(PropertyType.COMMERCIAL).landlord(landlord1).build());

        Property prop3 = save(Property.builder()
                .name("Greenview Residency").address("5 Park Lane, Capital City")
                .propertyType(PropertyType.RESIDENTIAL).landlord(landlord2).build());

        // ── Units ─────────────────────────────────────────────────────────────
        Unit u1A = saveUnit(prop1, "1A", "1", UnitStatus.OCCUPIED);
        Unit u1B = saveUnit(prop1, "1B", "1", UnitStatus.OCCUPIED);
        Unit u2A = saveUnit(prop1, "2A", "2", UnitStatus.VACANT);

        Unit c101 = saveUnit(prop2, "C101", "G", UnitStatus.OCCUPIED);
        Unit c102 = saveUnit(prop2, "C102", "G", UnitStatus.VACANT);

        Unit g1  = saveUnit(prop3, "G1", "G", UnitStatus.OCCUPIED);
        Unit g2  = saveUnit(prop3, "G2", "G", UnitStatus.VACANT);

        // ── Leases ────────────────────────────────────────────────────────────
        // ACTIVE lease
        Lease lease1 = seedLease(u1A, tenant1, landlord1,
                LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6),
                new BigDecimal("1200.00"), new BigDecimal("2400.00"),
                LeaseStatus.ACTIVE);

        // NOTICE_SERVED lease
        Lease lease2 = seedLease(u1B, tenant2, landlord1,
                LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(1),
                new BigDecimal("1100.00"), new BigDecimal("2200.00"),
                LeaseStatus.NOTICE_SERVED);

        // DRAFT lease
        Lease lease3 = seedLease(u2A, tenant3, landlord1,
                LocalDate.now().plusDays(5), LocalDate.now().plusMonths(12),
                new BigDecimal("950.00"), new BigDecimal("1900.00"),
                LeaseStatus.DRAFT);

        // TERMINATED lease on commercial unit
        Lease lease4 = seedLease(c101, tenant4, landlord1,
                LocalDate.now().minusMonths(8), LocalDate.now().minusMonths(1),
                new BigDecimal("3000.00"), new BigDecimal("6000.00"),
                LeaseStatus.TERMINATED);

        // SETTLED lease on g1
        Lease lease5 = seedLease(g1, tenant1, landlord2,
                LocalDate.now().minusMonths(14), LocalDate.now().minusMonths(2),
                new BigDecimal("800.00"), new BigDecimal("1600.00"),
                LeaseStatus.SETTLED);

        // ── Meter Readings (6 months for active leases) ───────────────────────
        seedReadings(u1A, UtilityType.ELECTRICITY, landlord1.getId());
        seedReadings(u1A, UtilityType.WATER,       landlord1.getId());
        seedReadings(u1B, UtilityType.ELECTRICITY, landlord1.getId());
        seedReadings(c101, UtilityType.ELECTRICITY, landlord1.getId());
        seedReadings(c101, UtilityType.GAS,         landlord1.getId());

        // ── Invoices ─────────────────────────────────────────────────────────
        YearMonth now = YearMonth.now();

        Invoice inv1 = seedInvoice(lease1, now.minusMonths(1),
                new BigDecimal("1200.00"), new BigDecimal("87.50"),
                InvoiceStatus.PAID, new BigDecimal("1287.50"));

        Invoice inv2 = seedInvoice(lease1, now,
                new BigDecimal("1200.00"), new BigDecimal("95.00"),
                InvoiceStatus.PENDING, BigDecimal.ZERO);

        Invoice inv3 = seedInvoice(lease2, now.minusMonths(1),
                new BigDecimal("1100.00"), new BigDecimal("62.00"),
                InvoiceStatus.OVERDUE, BigDecimal.ZERO);

        Invoice inv4 = seedInvoice(lease2, now.minusMonths(2),
                new BigDecimal("1100.00"), new BigDecimal("58.00"),
                InvoiceStatus.PARTIALLY_PAID, new BigDecimal("600.00"));

        // ── Payments ─────────────────────────────────────────────────────────
        seedPayment(inv1, new BigDecimal("1287.50"), PaymentMethod.BANK_TRANSFER);
        seedPayment(inv4, new BigDecimal("600.00"),  PaymentMethod.MOBILE_WALLET);

        printCredentials();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> T save(T entity) {
        if (entity instanceof User u)         return (T) userRepository.save(u);
        if (entity instanceof Property p)     return (T) propertyRepository.save(p);
        if (entity instanceof Invoice i)      return (T) invoiceRepository.save(i);
        if (entity instanceof Payment p)      return (T) paymentRepository.save(p);
        throw new IllegalArgumentException("Unsupported entity type");
    }

    private Unit saveUnit(Property property, String number, String floor, UnitStatus status) {
        Unit unit = Unit.builder()
                .property(property).unitNumber(number).floor(floor).status(status).build();
        return unitRepository.save(unit);
    }

    private Lease seedLease(Unit unit, User tenant, User actor,
                             LocalDate start, LocalDate end,
                             BigDecimal rent, BigDecimal deposit,
                             LeaseStatus targetStatus) {
        Lease lease = Lease.builder()
                .unit(unit).tenant(tenant)
                .startDate(start).endDate(end)
                .baseRent(rent).securityDeposit(deposit)
                .status(LeaseStatus.DRAFT).build();
        lease = leaseRepository.save(lease);

        // Apply transitions to reach targetStatus
        if (targetStatus == LeaseStatus.ACTIVE || targetStatus == LeaseStatus.NOTICE_SERVED
                || targetStatus == LeaseStatus.TERMINATED || targetStatus == LeaseStatus.SETTLED) {
            addHistory(lease, LeaseStatus.DRAFT, LeaseStatus.ACTIVE, actor);
            lease.setStatus(LeaseStatus.ACTIVE);
            unit.setStatus(UnitStatus.OCCUPIED);
            unitRepository.save(unit);
        }
        if (targetStatus == LeaseStatus.NOTICE_SERVED || targetStatus == LeaseStatus.TERMINATED
                || targetStatus == LeaseStatus.SETTLED) {
            addHistory(lease, LeaseStatus.ACTIVE, LeaseStatus.NOTICE_SERVED, actor);
            lease.setStatus(LeaseStatus.NOTICE_SERVED);
        }
        if (targetStatus == LeaseStatus.TERMINATED || targetStatus == LeaseStatus.SETTLED) {
            addHistory(lease, LeaseStatus.NOTICE_SERVED, LeaseStatus.TERMINATED, actor);
            lease.setStatus(LeaseStatus.TERMINATED);
            unit.setStatus(UnitStatus.VACANT);
            unitRepository.save(unit);
        }
        if (targetStatus == LeaseStatus.SETTLED) {
            addHistory(lease, LeaseStatus.TERMINATED, LeaseStatus.SETTLED, actor);
            lease.setStatus(LeaseStatus.SETTLED);
        }
        return leaseRepository.save(lease);
    }

    private void addHistory(Lease lease, LeaseStatus from, LeaseStatus to, User actor) {
        LeaseStatusHistory h = LeaseStatusHistory.builder()
                .lease(lease).fromStatus(from).toStatus(to)
                .changedBy(actor.getId()).changedAt(LocalDateTime.now()).build();
        lease.getStatusHistory().add(h);
    }

    private void seedReadings(Unit unit, UtilityType type, Long actorId) {
        BigDecimal current = BigDecimal.ZERO;
        for (int i = 5; i >= 0; i--) {
            BigDecimal prev = current;
            current = current.add(new BigDecimal(100 + (int)(Math.random() * 250)));
            MeterReading mr = MeterReading.builder()
                    .unit(unit).utilityType(type)
                    .previousReading(prev).currentReading(current)
                    .readingDate(LocalDate.now().minusMonths(i).withDayOfMonth(28))
                    .recordedBy(actorId).build();
            meterReadingRepository.save(mr);
        }
    }

    private Invoice seedInvoice(Lease lease, YearMonth month,
                                 BigDecimal baseRent, BigDecimal utilityTotal,
                                 InvoiceStatus status, BigDecimal amountPaid) {
        BigDecimal total = baseRent.add(utilityTotal);
        Invoice invoice = Invoice.builder()
                .lease(lease)
                .billingMonth(InvoiceService.toMonthInt(month))
                .baseRentAmount(baseRent)
                .utilityTotalAmount(utilityTotal)
                .fineAmount(BigDecimal.ZERO)
                .totalPayable(total)
                .amountPaid(amountPaid)
                .status(status)
                .dueDate(month.atEndOfMonth())
                .build();

        InvoiceLineItem rentLine = InvoiceLineItem.builder()
                .description("Base Rent — " + month)
                .amount(baseRent).build();
        InvoiceLineItem utilLine = InvoiceLineItem.builder()
                .description("Utilities — " + month)
                .utilityType(UtilityType.ELECTRICITY)
                .unitsConsumed(new BigDecimal("350"))
                .amount(utilityTotal).build();
        invoice.getLineItems().add(rentLine);
        invoice.getLineItems().add(utilLine);
        rentLine.setInvoice(invoice);
        utilLine.setInvoice(invoice);

        return invoiceRepository.save(invoice);
    }

    private void seedPayment(Invoice invoice, BigDecimal amount, PaymentMethod method) {
        Payment payment = Payment.builder()
                .invoice(invoice).amountPaid(amount)
                .transactionReference("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .paymentMethod(method).paidAt(LocalDateTime.now().minusDays(3)).build();
        paymentRepository.save(payment);
    }

    private void printCredentials() {
        log.info("\n" +
                "╔══════════════════════════════════════════════════════════╗\n" +
                "║          LeaseFlow — Demo Credentials                    ║\n" +
                "╠══════════════════════════════════════════════════════════╣\n" +
                "║  admin@leaseflow.dev      password: Admin@123    (ADMIN) ║\n" +
                "║  landlord1@leaseflow.dev  password: Landlord@123         ║\n" +
                "║  landlord2@leaseflow.dev  password: Landlord@123         ║\n" +
                "║  tenant1@leaseflow.dev    password: Tenant@123           ║\n" +
                "║  tenant2@leaseflow.dev    password: Tenant@123           ║\n" +
                "║  tenant3@leaseflow.dev    password: Tenant@123           ║\n" +
                "║  tenant4@leaseflow.dev    password: Tenant@123           ║\n" +
                "╚══════════════════════════════════════════════════════════╝\n" +
                "  Swagger UI: http://localhost:8080/swagger-ui.html"
        );
    }
}

