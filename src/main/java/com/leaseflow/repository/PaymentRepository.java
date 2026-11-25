package com.leaseflow.repository;

import com.leaseflow.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByTransactionReference(String transactionReference);

    List<Payment> findByInvoiceIdOrderByPaidAtDesc(Long invoiceId);

    /** All payments made by a tenant, across all their invoices. */
    @Query("SELECT p FROM Payment p WHERE p.invoice.lease.tenant.id = :tenantId " +
           "ORDER BY p.paidAt DESC")
    Page<Payment> findByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.invoice.lease.tenant.id = :tenantId " +
           "ORDER BY p.paidAt DESC")
    List<Payment> findTop5ByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    /** Verify a payment belongs to a tenant (for security checks). */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.id = :id AND p.invoice.lease.tenant.id = :tenantId")
    long countByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}

