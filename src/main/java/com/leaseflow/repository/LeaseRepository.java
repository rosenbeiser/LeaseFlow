package com.leaseflow.repository;

import com.leaseflow.entity.Lease;
import com.leaseflow.enums.LeaseStatus;
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
public interface LeaseRepository extends JpaRepository<Lease, Long> {

    /** All leases on units owned by a landlord, optionally filtered by status. */
    @Query("SELECT l FROM Lease l WHERE l.unit.property.landlord.id = :landlordId " +
           "AND (:status IS NULL OR l.status = :status)")
    Page<Lease> findByLandlordId(@Param("landlordId") Long landlordId,
                                  @Param("status") LeaseStatus status,
                                  Pageable pageable);

    /** Tenant's own leases. */
    Page<Lease> findByTenantId(Long tenantId, Pageable pageable);

    List<Lease> findByTenantId(Long tenantId);

    Optional<Lease> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT l FROM Lease l WHERE l.unit.property.landlord.id = :landlordId AND l.id = :id")
    Optional<Lease> findByIdAndLandlordId(@Param("id") Long id, @Param("landlordId") Long landlordId);

    /**
     * Detects overlapping active leases on the same unit.
     * Excludes SETTLED and TERMINATED statuses and a specific lease id (for updates).
     */
    @Query("SELECT COUNT(l) FROM Lease l WHERE l.unit.id = :unitId " +
           "AND l.id <> :excludeId " +
           "AND l.status NOT IN (com.leaseflow.enums.LeaseStatus.SETTLED, com.leaseflow.enums.LeaseStatus.TERMINATED) " +
           "AND l.startDate < :endDate AND l.endDate > :startDate")
    long countOverlappingLeases(@Param("unitId") Long unitId,
                                 @Param("excludeId") Long excludeId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(l) FROM Lease l WHERE l.unit.property.landlord.id = :landlordId " +
           "AND l.status = :status")
    long countByLandlordIdAndStatus(@Param("landlordId") Long landlordId, @Param("status") LeaseStatus status);

    long countByStatus(LeaseStatus status);

    @Query("SELECT l FROM Lease l WHERE (:status IS NULL OR l.status = :status)")
    Page<Lease> findAllWithFilter(@Param("status") LeaseStatus status, Pageable pageable);
}
