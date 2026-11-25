package com.leaseflow.entity;

import com.leaseflow.enums.UtilityType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Itemized line within an {@link Invoice}, providing the breakdown that is rendered
 * in the invoice detail view and printed invoice. Each utility reading generates one
 * line item; base rent generates another.
 */
@Entity
@Table(name = "invoice_line_items",
        indexes = @Index(name = "idx_ili_invoice", columnList = "invoice_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false, length = 300)
    private String description;

    /** Null for base-rent line items; set for utility line items. */
    @Enumerated(EnumType.STRING)
    @Column(name = "utility_type", length = 20)
    private UtilityType utilityType;

    /** Consumption in metered units; null for base-rent line items. */
    @Column(name = "units_consumed", precision = 12, scale = 2)
    private BigDecimal unitsConsumed;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
}

