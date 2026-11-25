package com.leaseflow.repository;

import com.leaseflow.entity.Invoice;
import com.leaseflow.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByLeaseIdAndBillingMonth(Long leaseId, Integer billingMonth);

    List<Invoice> findByLeaseId(Long leaseId);

    /** Landlord invoice listing with optional status and billing month filters. */
    @Query("SELECT i FROM Invoice i WHERE i.lease.unit.property.landlord.id = :landlordId " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:month IS NULL OR i.billingMonth = :month)")
    Page<Invoice> findByLandlordId(@Param("landlordId") Long landlordId,
                                    @Param("status") InvoiceStatus status,
                                    @Param("month") Integer month,
                                    Pageable pageable);

    /** Platform-wide invoice listing with optional status and billing month filters (for ADMIN). */
    @Query("SELECT i FROM Invoice i WHERE (:status IS NULL OR i.status = :status) " +
           "AND (:month IS NULL OR i.billingMonth = :month)")
    Page<Invoice> findAllWithFilters(@Param("status") InvoiceStatus status,
                                     @Param("month") Integer month,
                                     Pageable pageable);

    /** Tenant's own invoices with optional status filter. */
    @Query("SELECT i FROM Invoice i WHERE i.lease.tenant.id = :tenantId " +
           "AND (:status IS NULL OR i.status = :status)")
    Page<Invoice> findByTenantId(@Param("tenantId") Long tenantId,
                                  @Param("status") InvoiceStatus status,
                                  Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.id = :id AND i.lease.tenant.id = :tenantId")
    Optional<Invoice> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT i FROM Invoice i WHERE i.id = :id AND i.lease.unit.property.landlord.id = :landlordId")
    Optional<Invoice> findByIdAndLandlordId(@Param("id") Long id, @Param("landlordId") Long landlordId);

    /** Count overdue invoices for a landlord's portfolio. */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.lease.unit.property.landlord.id = :landlordId " +
           "AND i.status = com.leaseflow.enums.InvoiceStatus.OVERDUE")
    long countOverdueByLandlordId(@Param("landlordId") Long landlordId);

    /** Count overdue invoices platform-wide (for ADMIN). */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.status = com.leaseflow.enums.InvoiceStatus.OVERDUE")
    long countAllOverdue();

    /** Total outstanding balance across a landlord's portfolio. */
    @Query("SELECT COALESCE(SUM(i.totalPayable - i.amountPaid), 0) FROM Invoice i " +
           "WHERE i.lease.unit.property.landlord.id = :landlordId " +
           "AND i.status IN (com.leaseflow.enums.InvoiceStatus.PENDING, com.leaseflow.enums.InvoiceStatus.PARTIALLY_PAID, com.leaseflow.enums.InvoiceStatus.OVERDUE)")
    BigDecimal sumOutstandingByLandlordId(@Param("landlordId") Long landlordId);

    /** Total outstanding balance platform-wide (for ADMIN). */
    @Query("SELECT COALESCE(SUM(i.totalPayable - i.amountPaid), 0) FROM Invoice i " +
           "WHERE i.status IN (com.leaseflow.enums.InvoiceStatus.PENDING, com.leaseflow.enums.InvoiceStatus.PARTIALLY_PAID, com.leaseflow.enums.InvoiceStatus.OVERDUE)")
    BigDecimal sumAllOutstanding();

    /** Total outstanding balance for a tenant. */
    @Query("SELECT COALESCE(SUM(i.totalPayable - i.amountPaid), 0) FROM Invoice i " +
           "WHERE i.lease.tenant.id = :tenantId " +
           "AND i.status IN (com.leaseflow.enums.InvoiceStatus.PENDING, com.leaseflow.enums.InvoiceStatus.PARTIALLY_PAID, com.leaseflow.enums.InvoiceStatus.OVERDUE)")
    BigDecimal sumOutstandingByTenantId(@Param("tenantId") Long tenantId);

    /** Invoices past due date that are not yet marked OVERDUE (for the scheduler). */
    @Query("SELECT i FROM Invoice i WHERE i.dueDate < :today " +
           "AND i.status IN (com.leaseflow.enums.InvoiceStatus.PENDING, com.leaseflow.enums.InvoiceStatus.PARTIALLY_PAID)")
    List<Invoice> findUnpaidPastDue(@Param("today") LocalDate today);

    /** Revenue collected per month (for dashboard chart). */
    @Query("SELECT i.billingMonth, COALESCE(SUM(i.amountPaid), 0) FROM Invoice i " +
           "WHERE i.lease.unit.property.landlord.id = :landlordId " +
           "AND i.billingMonth >= :fromMonth " +
           "GROUP BY i.billingMonth ORDER BY i.billingMonth ASC")
    List<Object[]> revenueByMonthForLandlord(@Param("landlordId") Long landlordId,
                                              @Param("fromMonth") Integer fromMonth);

    /** Revenue collected per month platform-wide (for ADMIN). */
    @Query("SELECT i.billingMonth, COALESCE(SUM(i.amountPaid), 0) FROM Invoice i " +
           "WHERE i.billingMonth >= :fromMonth " +
           "GROUP BY i.billingMonth ORDER BY i.billingMonth ASC")
    List<Object[]> revenueByMonthPlatform(@Param("fromMonth") Integer fromMonth);

    /** Unpaid balance on a lease (used during settlement calculation). */
    @Query("SELECT COALESCE(SUM(i.totalPayable - i.amountPaid), 0) FROM Invoice i " +
           "WHERE i.lease.id = :leaseId AND i.status IN (com.leaseflow.enums.InvoiceStatus.PENDING, com.leaseflow.enums.InvoiceStatus.PARTIALLY_PAID, com.leaseflow.enums.InvoiceStatus.OVERDUE)")
    BigDecimal sumUnpaidByLeaseId(@Param("leaseId") Long leaseId);

    /** 5 most recent invoices for a landlord (dashboard widget). */
    @Query("SELECT i FROM Invoice i WHERE i.lease.unit.property.landlord.id = :landlordId " +
           "ORDER BY i.createdAt DESC")
    List<Invoice> findTop5ByLandlordId(@Param("landlordId") Long landlordId, Pageable pageable);

    /** 5 most recent invoices platform-wide (for ADMIN dashboard widget). */
    @Query("SELECT i FROM Invoice i ORDER BY i.createdAt DESC")
    List<Invoice> findTop5Platform(Pageable pageable);

    /** Next invoice due for a tenant (dashboard widget). */
    @Query("SELECT i FROM Invoice i WHERE i.lease.tenant.id = :tenantId " +
           "AND i.status IN (com.leaseflow.enums.InvoiceStatus.PENDING, com.leaseflow.enums.InvoiceStatus.PARTIALLY_PAID, com.leaseflow.enums.InvoiceStatus.OVERDUE) " +
           "ORDER BY i.dueDate ASC")
    List<Invoice> findNextDueForTenant(@Param("tenantId") Long tenantId, Pageable pageable);
}
