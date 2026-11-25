package com.leaseflow.repository;

import com.leaseflow.entity.Unit;
import com.leaseflow.enums.UnitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findByPropertyId(Long propertyId);

    Page<Unit> findByPropertyId(Long propertyId, Pageable pageable);

    Optional<Unit> findByIdAndPropertyLandlordId(Long id, Long landlordId);

    boolean existsByUnitNumberAndPropertyId(String unitNumber, Long propertyId);

    /** Count units by status within a landlord's portfolio. */
    @Query("SELECT COUNT(u) FROM Unit u WHERE u.property.landlord.id = :landlordId AND u.status = :status")
    long countByLandlordIdAndStatus(@Param("landlordId") Long landlordId, @Param("status") UnitStatus status);

    @Query("SELECT COUNT(u) FROM Unit u WHERE u.property.landlord.id = :landlordId")
    long countByLandlordId(@Param("landlordId") Long landlordId);

    long countByStatus(UnitStatus status);
}
