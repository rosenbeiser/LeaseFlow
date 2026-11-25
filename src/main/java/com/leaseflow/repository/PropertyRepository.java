package com.leaseflow.repository;

import com.leaseflow.entity.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    Page<Property> findByLandlordId(Long landlordId, Pageable pageable);

    Optional<Property> findByIdAndLandlordId(Long id, Long landlordId);

    boolean existsByIdAndLandlordId(Long id, Long landlordId);

    long countByLandlordId(Long landlordId);
}
