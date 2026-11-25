package com.leaseflow.repository;

import com.leaseflow.entity.MeterReading;
import com.leaseflow.enums.UtilityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {

    Page<MeterReading> findByUnitId(Long unitId, Pageable pageable);

    /** Find the most recent reading before a given date for auto-derivation of previousReading. */
    @Query("SELECT m FROM MeterReading m WHERE m.unit.id = :unitId AND m.utilityType = :type " +
           "AND m.readingDate < :beforeDate ORDER BY m.readingDate DESC, m.id DESC")
    List<MeterReading> findLatestBefore(@Param("unitId") Long unitId,
                                         @Param("type") UtilityType type,
                                         @Param("beforeDate") LocalDate beforeDate,
                                         Pageable pageable);

    /** Readings for a unit and utility type within a billing month (for invoice generation). */
    @Query("SELECT m FROM MeterReading m WHERE m.unit.id = :unitId AND m.utilityType = :type " +
           "AND m.readingDate >= :monthStart AND m.readingDate <= :monthEnd " +
           "ORDER BY m.readingDate DESC, m.id DESC")
    List<MeterReading> findByUnitAndTypeInMonth(@Param("unitId") Long unitId,
                                                 @Param("type") UtilityType type,
                                                 @Param("monthStart") LocalDate monthStart,
                                                 @Param("monthEnd") LocalDate monthEnd);

    /** All reading types present for a unit in a given month (drives line-item creation). */
    @Query("SELECT DISTINCT m.utilityType FROM MeterReading m WHERE m.unit.id = :unitId " +
           "AND m.readingDate >= :monthStart AND m.readingDate <= :monthEnd")
    List<UtilityType> findDistinctUtilityTypesByUnitInMonth(@Param("unitId") Long unitId,
                                                              @Param("monthStart") LocalDate monthStart,
                                                              @Param("monthEnd") LocalDate monthEnd);

    /** Verify landlord ownership before allowing a reading query. */
    @Query("SELECT COUNT(m) FROM MeterReading m WHERE m.unit.id = :unitId " +
           "AND m.unit.property.landlord.id = :landlordId")
    long countByUnitIdAndLandlordId(@Param("unitId") Long unitId, @Param("landlordId") Long landlordId);
}

