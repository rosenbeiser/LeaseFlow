package com.leaseflow.entity;

import com.leaseflow.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Monthly billing statement for a {@link Lease}. Each invoice is unique per
 * (lease, billingMonth) – enforced via a composite unique constraint and a
 * service-level check that returns HTTP 409 on duplicate generation attempts.
 *
 * <p>Amount breakdown:
 * <pre>
 *   totalPayable = baseRentAmount + utilityTotalAmount + fineAmount
 *   outstanding  = totalPayable - amountPaid
 * </pre>
 * The {@code fineAmount} is updated by a daily {@code @Scheduled} job in
 * {@code InvoiceService} at rate {@code app.billing.late-fine-rate} (default 2%).
 */
@Entity
@Table(name = "invoices",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_invoice_lease_month",
                columnNames = {"lease_id", "billing_month"}),
        indexes = {
                @Index(name = "idx_invoice_lease", columnList = "lease_id"),
                @Index(name = "idx_invoice_status", columnList = "status"),
                @Index(name = "idx_invoice_due_date", columnList = "due_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lease_id", nullable = false)
    private Lease lease;

    /**
     * Billing period stored as integer YYYYMM for portability across databases.
     * Converted to/from {@link YearMonth} in the service layer.
     */
    @Column(name = "billing_month", nullable = false)
    private Integer billingMonth;

    @Column(name = "base_rent_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseRentAmount;

    @Column(name = "utility_total_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal utilityTotalAmount = BigDecimal.ZERO;

    @Column(name = "fine_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal fineAmount = BigDecimal.ZERO;

    @Column(name = "total_payable", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPayable;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("paid_at DESC")
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();
}

