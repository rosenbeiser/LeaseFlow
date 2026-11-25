package com.leaseflow.entity;

import com.leaseflow.enums.LeaseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Immutable audit record written on every {@link Lease} state transition.
 * Powers the vertical lifecycle timeline displayed in the landlord's Lease Detail UI.
 * Records the actor ({@code changedBy}) who triggered the transition.
 */
@Entity
@Table(name = "lease_status_history",
        indexes = @Index(name = "idx_lsh_lease", columnList = "lease_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaseStatusHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lease_id", nullable = false)
    private Lease lease;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 20)
    private LeaseStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private LeaseStatus toStatus;

    /** ID of the User who triggered the state change (not a FK to avoid loading overhead). */
    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(length = 500)
    private String notes;
}

