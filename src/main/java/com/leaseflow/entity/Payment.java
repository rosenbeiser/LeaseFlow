package com.leaseflow.entity;

import com.leaseflow.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a single payment transaction against an {@link Invoice}.
 * {@code transactionReference} must be globally unique (enforced by DB constraint
 * and service-level validation). Multiple payments against one invoice are allowed
 * until the full balance is cleared.
 *
 * <p>Business rules:
 * <ul>
 *   <li>amountPaid must be > 0.</li>
 *   <li>amountPaid must be ≤ (invoice.totalPayable - invoice.amountPaid).</li>
 *   <li>After payment, invoice status updates to PARTIALLY_PAID or PAID.</li>
 * </ul>
 */
@Entity
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_payment_invoice", columnList = "invoice_id"),
                @Index(name = "idx_payment_txref", columnList = "transaction_reference", unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 100)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;
}

