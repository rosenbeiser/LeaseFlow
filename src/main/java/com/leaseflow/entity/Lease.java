package com.leaseflow.entity;

import com.leaseflow.enums.LeaseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a rental agreement between a Landlord (via a {@link Unit}) and a
 * {@link User} with role TENANT. The lifecycle is governed by the State Pattern
 * ({@code com.leaseflow.pattern.state}) which enforces valid transitions and
 * writes an audit row to {@link LeaseStatusHistory} on every change.
 *
 * <p>Business invariants (enforced in {@code LeaseService}):
 * <ul>
 *   <li>endDate must be strictly after startDate.</li>
 *   <li>No two non-SETTLED/non-TERMINATED leases may overlap on the same unit.</li>
 *   <li>baseRent and securityDeposit must be positive.</li>
 * </ul>
 */
@Entity
@Table(name = "leases",
        indexes = {
                @Index(name = "idx_lease_unit", columnList = "unit_id"),
                @Index(name = "idx_lease_tenant", columnList = "tenant_id"),
                @Index(name = "idx_lease_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lease extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal baseRent;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal securityDeposit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LeaseStatus status = LeaseStatus.DRAFT;

    @OneToMany(mappedBy = "lease", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    @Builder.Default
    private List<LeaseStatusHistory> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "lease", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Invoice> invoices = new ArrayList<>();
}

