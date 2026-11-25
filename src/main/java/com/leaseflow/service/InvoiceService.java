package com.leaseflow.service;

import com.leaseflow.entity.*;
import com.leaseflow.enums.InvoiceStatus;
import com.leaseflow.enums.UtilityType;
import com.leaseflow.exception.BusinessRuleException;
import com.leaseflow.exception.DuplicateResourceException;
import com.leaseflow.exception.ResourceNotFoundException;
import com.leaseflow.pattern.strategy.BillingStrategy;
import com.leaseflow.pattern.strategy.BillingStrategyFactory;
import com.leaseflow.repository.InvoiceRepository;
import com.leaseflow.repository.MeterReadingRepository;
import com.leaseflow.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final PaymentRepository paymentRepository;
    private final LeaseService leaseService;
    private final BillingStrategyFactory billingStrategyFactory;

    @Value("${app.billing.late-fine-rate:0.02}")
    private BigDecimal lateFineRate;

    // ── Queries ───────────────────────────────────────────────────────────────

    public Page<Invoice> findByLandlord(Long landlordId, InvoiceStatus status,
                                         Integer billingMonth, Pageable pageable) {
        return invoiceRepository.findByLandlordId(landlordId, status, billingMonth, pageable);
    }

    public Page<Invoice> findAllPlatform(InvoiceStatus status, Integer billingMonth, Pageable pageable) {
        return invoiceRepository.findAllWithFilters(status, billingMonth, pageable);
    }

    public Page<Invoice> findByTenant(Long tenantId, InvoiceStatus status, Pageable pageable) {
        return invoiceRepository.findByTenantId(tenantId, status, pageable);
    }

    public List<Invoice> findByLeaseId(Long leaseId) {
        return invoiceRepository.findByLeaseId(leaseId);
    }

    public Invoice findByIdAndLandlord(Long id, Long landlordId) {
        return invoiceRepository.findByIdAndLandlordId(id, landlordId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found or you do not have access to it."));
    }

    public Invoice findByIdAndTenant(Long id, Long tenantId) {
        return invoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found or you do not have access to it."));
    }

    public Invoice findById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
    }

    public Invoice findNextDueForTenant(Long tenantId) {
        List<Invoice> list = invoiceRepository.findNextDueForTenant(
                tenantId, PageRequest.of(0, 1));
        return list.isEmpty() ? null : list.get(0);
    }

    // ── Invoice Generation ────────────────────────────────────────────────────

    @Transactional
    public Invoice generateInvoice(Long leaseId, YearMonth billingMonth, User actor) {
        Lease lease = leaseService.findById(leaseId);

        // 1. Verify state permits invoice generation
        leaseService.assertInvoiceAllowed(lease, actor);

        // 2. Guard against duplicate
        int monthInt = toMonthInt(billingMonth);
        if (invoiceRepository.existsByLeaseIdAndBillingMonth(leaseId, monthInt)) {
            throw new DuplicateResourceException(
                    "Invoice already exists for lease " + leaseId + " and month " + billingMonth);
        }

        // 3. Build line items
        List<InvoiceLineItem> lineItems = new ArrayList<>();
        BigDecimal utilityTotal = BigDecimal.ZERO;

        // Base-rent line item
        InvoiceLineItem rentLine = InvoiceLineItem.builder()
                .description("Base Rent — " + billingMonth)
                .amount(lease.getBaseRent())
                .build();
        lineItems.add(rentLine);

        // Utility line items
        LocalDate monthStart = billingMonth.atDay(1);
        LocalDate monthEnd   = billingMonth.atEndOfMonth();

        List<UtilityType> utilityTypes = meterReadingRepository
                .findDistinctUtilityTypesByUnitInMonth(
                        lease.getUnit().getId(), monthStart, monthEnd);

        for (UtilityType utilityType : utilityTypes) {
            List<MeterReading> readings = meterReadingRepository.findByUnitAndTypeInMonth(
                    lease.getUnit().getId(), utilityType, monthStart, monthEnd);

            if (readings.isEmpty()) continue;

            MeterReading latest = readings.get(0); // already ordered DESC
            BigDecimal consumed = latest.getCurrentReading()
                    .subtract(latest.getPreviousReading())
                    .max(BigDecimal.ZERO);

            BillingStrategy strategy = billingStrategyFactory.getStrategy(
                    lease.getUnit().getProperty().getPropertyType(), utilityType);

            BigDecimal charge = strategy.calculateUtilityCharge(consumed);

            InvoiceLineItem utilityLine = InvoiceLineItem.builder()
                    .description(utilityType.name() + " — " + strategy.getDescription())
                    .utilityType(utilityType)
                    .unitsConsumed(consumed)
                    .amount(charge)
                    .build();
            lineItems.add(utilityLine);
            utilityTotal = utilityTotal.add(charge);
        }

        // 4. Assemble invoice
        BigDecimal totalPayable = lease.getBaseRent()
                .add(utilityTotal)
                .setScale(2, RoundingMode.HALF_UP);

        Invoice invoice = Invoice.builder()
                .lease(lease)
                .billingMonth(monthInt)
                .baseRentAmount(lease.getBaseRent())
                .utilityTotalAmount(utilityTotal.setScale(2, RoundingMode.HALF_UP))
                .fineAmount(BigDecimal.ZERO)
                .totalPayable(totalPayable)
                .amountPaid(BigDecimal.ZERO)
                .status(InvoiceStatus.PENDING)
                .dueDate(billingMonth.atEndOfMonth())
                .build();

        for (InvoiceLineItem item : lineItems) {
            item.setInvoice(invoice);
        }
        invoice.getLineItems().addAll(lineItems);

        return invoiceRepository.save(invoice);
    }

    // ── Payment Processing ────────────────────────────────────────────────────

    @Transactional
    public Payment recordPayment(Long invoiceId, BigDecimal amount, String method,
                                  String txRef, Long tenantId) {
        Invoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found or you do not have access to it."));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessRuleException("This invoice is already fully paid.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Payment amount must be greater than zero.");
        }

        BigDecimal outstanding = invoice.getTotalPayable().subtract(invoice.getAmountPaid());
        if (amount.compareTo(outstanding) > 0) {
            throw new BusinessRuleException(
                    "Payment amount (" + amount + ") exceeds outstanding balance (" + outstanding + ").");
        }
        if (paymentRepository.existsByTransactionReference(txRef)) {
            throw new DuplicateResourceException(
                    "Transaction reference '" + txRef + "' already exists.");
        }

        Payment payment = Payment.builder()
                .invoice(invoice)
                .amountPaid(amount)
                .transactionReference(txRef)
                .paymentMethod(com.leaseflow.enums.PaymentMethod.valueOf(method))
                .paidAt(java.time.LocalDateTime.now())
                .build();

        invoice.setAmountPaid(invoice.getAmountPaid().add(amount));
        BigDecimal newOutstanding = invoice.getTotalPayable().subtract(invoice.getAmountPaid());

        if (newOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        invoiceRepository.save(invoice);
        return paymentRepository.save(payment);
    }

    // ── Settlement Calculation ────────────────────────────────────────────────

    /**
     * Calculates the security-deposit refund at lease settlement.
     * {@code refund = securityDeposit - unpaidInvoiceBalance - damagesAmount}
     * A negative value means the tenant still owes that amount.
     */
    public BigDecimal calculateRefund(Long leaseId, BigDecimal damagesAmount) {
        Lease lease = leaseService.findById(leaseId);
        BigDecimal unpaid = invoiceRepository.sumUnpaidByLeaseId(leaseId);
        BigDecimal damages = (damagesAmount == null) ? BigDecimal.ZERO : damagesAmount;
        return lease.getSecurityDeposit()
                .subtract(unpaid)
                .subtract(damages)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ── Scheduled Fine / Overdue Job ──────────────────────────────────────────

    /**
     * Runs daily at midnight UTC. For each unpaid invoice past its due date:
     * <ol>
     *   <li>Marks status as OVERDUE.</li>
     *   <li>Applies a fine of {@code lateFineRate × totalPayable} per overdue month.</li>
     * </ol>
     * The fine is additive per run (compounding monthly effect) and is capped to
     * prevent runaway charges. Rate is configurable via {@code app.billing.late-fine-rate}.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    @Transactional
    public void applyLateFinesAndMarkOverdue() {
        LocalDate today = LocalDate.now();
        List<Invoice> pastDue = invoiceRepository.findUnpaidPastDue(today);
        log.info("Late-fine scheduler: processing {} overdue invoices", pastDue.size());

        for (Invoice invoice : pastDue) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
            // Apply 2% of totalPayable as a monthly late fine
            BigDecimal fine = invoice.getTotalPayable()
                    .multiply(lateFineRate)
                    .setScale(2, RoundingMode.HALF_UP);
            invoice.setFineAmount(invoice.getFineAmount().add(fine));
            invoice.setTotalPayable(invoice.getTotalPayable().add(fine));
            invoiceRepository.save(invoice);
        }
    }

    // ── Dashboard Helpers ─────────────────────────────────────────────────────

    public long countOverdueByLandlord(Long landlordId) {
        return invoiceRepository.countOverdueByLandlordId(landlordId);
    }

    public long countAllOverdue() {
        return invoiceRepository.countAllOverdue();
    }

    public BigDecimal sumOutstandingByLandlord(Long landlordId) {
        return invoiceRepository.sumOutstandingByLandlordId(landlordId);
    }

    public BigDecimal sumAllOutstanding() {
        return invoiceRepository.sumAllOutstanding();
    }

    public BigDecimal sumOutstandingByTenant(Long tenantId) {
        return invoiceRepository.sumOutstandingByTenantId(tenantId);
    }

    public List<Object[]> revenueByMonth(Long landlordId, int fromMonth) {
        return invoiceRepository.revenueByMonthForLandlord(landlordId, fromMonth);
    }

    public List<Object[]> revenueByMonthPlatform(int fromMonth) {
        return invoiceRepository.revenueByMonthPlatform(fromMonth);
    }

    public List<Invoice> getRecentInvoicesForLandlord(Long landlordId) {
        return invoiceRepository.findTop5ByLandlordId(landlordId, PageRequest.of(0, 5));
    }

    public List<Invoice> getRecentInvoicesPlatform() {
        return invoiceRepository.findTop5Platform(PageRequest.of(0, 5));
    }

    // ── Billing Estimate ─────────────────────────────────────────────────────

    public BigDecimal estimateCharge(com.leaseflow.enums.PropertyType propertyType,
                                      com.leaseflow.enums.UtilityType utilityType,
                                      BigDecimal units) {
        BillingStrategy strategy = billingStrategyFactory.getStrategy(propertyType, utilityType);
        return strategy.calculateUtilityCharge(units);
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    public static int toMonthInt(YearMonth ym) {
        return ym.getYear() * 100 + ym.getMonthValue();
    }

    public static YearMonth fromMonthInt(int monthInt) {
        return YearMonth.of(monthInt / 100, monthInt % 100);
    }
}

