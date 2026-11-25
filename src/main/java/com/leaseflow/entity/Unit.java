package com.leaseflow.entity;

import com.leaseflow.enums.UnitStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A rentable unit within a {@link Property}. The combination of
 * {@code (property, unitNumber)} must be unique – enforced via a composite
 * unique constraint. Status transitions are driven by the Lease state machine:
 * VACANT → OCCUPIED on activation, OCCUPIED → VACANT on termination.
 */
@Entity
@Table(name = "units",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_unit_number_property",
                columnNames = {"unit_number", "property_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Unit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_number", nullable = false, length = 50)
    private String unitNumber;

    @Column(length = 10)
    private String floor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UnitStatus status = UnitStatus.VACANT;

    @OneToMany(mappedBy = "unit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Lease> leases = new ArrayList<>();

    @OneToMany(mappedBy = "unit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MeterReading> meterReadings = new ArrayList<>();
}

