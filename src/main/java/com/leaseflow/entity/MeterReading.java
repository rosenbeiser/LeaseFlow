package com.leaseflow.entity;

import com.leaseflow.enums.UtilityType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Records a utility meter reading for a {@link Unit}. During invoice generation
 * the service queries for readings whose {@code readingDate} falls within the
 * target billing month and derives consumption as
 * {@code currentReading - previousReading}.
 *
 * <p>Invariants:
 * <ul>
 *   <li>{@code currentReading >= previousReading} (validated in service layer).</li>
 *   <li>{@code previousReading} may be auto-derived from the latest prior reading
 *       when not supplied in the request.</li>
 * </ul>
 */
@Entity
@Table(name = "meter_readings",
        indexes = {
                @Index(name = "idx_mr_unit_type_date", columnList = "unit_id, utility_type, reading_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterReading extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "utility_type", nullable = false, length = 20)
    private UtilityType utilityType;

    @Column(name = "previous_reading", nullable = false, precision = 12, scale = 2)
    private BigDecimal previousReading;

    @Column(name = "current_reading", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentReading;

    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    /** ID of the User (Landlord) who recorded this reading. */
    @Column(name = "recorded_by", nullable = false)
    private Long recordedBy;
}

