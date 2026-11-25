package com.leaseflow.service;

import com.leaseflow.entity.Lease;
import com.leaseflow.entity.Unit;
import com.leaseflow.entity.User;
import com.leaseflow.enums.LeaseStatus;
import com.leaseflow.exception.BusinessRuleException;
import com.leaseflow.exception.DuplicateResourceException;
import com.leaseflow.exception.ResourceNotFoundException;
import com.leaseflow.pattern.state.LeaseState;
import com.leaseflow.pattern.state.LeaseStateFactory;
import com.leaseflow.repository.InvoiceRepository;
import com.leaseflow.repository.LeaseRepository;
import com.leaseflow.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaseService {

    private final LeaseRepository leaseRepository;
    private final UnitRepository unitRepository;
    private final InvoiceRepository invoiceRepository;

    // ── Queries ───────────────────────────────────────────────────────────────

    public Page<Lease> findByLandlord(Long landlordId, LeaseStatus status, Pageable pageable) {
        return leaseRepository.findByLandlordId(landlordId, status, pageable);
    }

    public Page<Lease> findAllWithFilter(LeaseStatus status, Pageable pageable) {
        return leaseRepository.findAllWithFilter(status, pageable);
    }

    public Page<Lease> findByTenant(Long tenantId, Pageable pageable) {
        return leaseRepository.findByTenantId(tenantId, pageable);
    }

    public List<Lease> findAllByTenant(Long tenantId) {
        return leaseRepository.findByTenantId(tenantId);
    }

    public Lease findByIdAndLandlord(Long id, Long landlordId) {
        return leaseRepository.findByIdAndLandlordId(id, landlordId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lease not found or you do not have access to it."));
    }

    public Lease findByIdAndTenant(Long id, Long tenantId) {
        return leaseRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lease not found or you do not have access to it."));
    }

    public Lease findById(Long id) {
        return leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", id));
    }

    public List<String> getAllowedActions(Lease lease) {
        return LeaseStateFactory.getState(lease).getAllowedActions();
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @Transactional
    public Lease createLease(Lease lease) {
        validateDates(lease);
        validateNoOverlap(lease, -1L);
        return leaseRepository.save(lease);
    }

    @Transactional
    public Lease activate(Long leaseId, User actor) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", leaseId));
        LeaseState state = LeaseStateFactory.getState(lease);
        state.activate(lease, actor);
        unitRepository.save(lease.getUnit());
        return leaseRepository.save(lease);
    }

    @Transactional
    public Lease serveNotice(Long leaseId, User actor) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", leaseId));
        LeaseState state = LeaseStateFactory.getState(lease);
        state.serveNotice(lease, actor);
        return leaseRepository.save(lease);
    }

    @Transactional
    public Lease terminate(Long leaseId, User actor) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", leaseId));
        LeaseState state = LeaseStateFactory.getState(lease);
        state.terminate(lease, actor);
        unitRepository.save(lease.getUnit());
        return leaseRepository.save(lease);
    }

    @Transactional
    public Lease settle(Long leaseId, User actor, BigDecimal damagesAmount, String notes) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", leaseId));
        LeaseState state = LeaseStateFactory.getState(lease);
        state.settle(lease, actor, damagesAmount, notes);
        return leaseRepository.save(lease);
    }

    /** Validate that the state permits invoice generation (delegates to state machine). */
    public void assertInvoiceAllowed(Lease lease, User actor) {
        LeaseStateFactory.getState(lease).generateInvoice(lease, actor);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateDates(Lease lease) {
        if (!lease.getEndDate().isAfter(lease.getStartDate())) {
            throw new BusinessRuleException("End date must be strictly after start date.");
        }
        if (lease.getBaseRent().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Base rent must be positive.");
        }
        if (lease.getSecurityDeposit().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Security deposit cannot be negative.");
        }
    }

    private void validateNoOverlap(Lease lease, Long excludeId) {
        long overlaps = leaseRepository.countOverlappingLeases(
                lease.getUnit().getId(),
                excludeId < 0 ? Long.MIN_VALUE : excludeId,
                lease.getStartDate(),
                lease.getEndDate());
        if (overlaps > 0) {
            throw new DuplicateResourceException(
                    "An active lease already exists for this unit in the specified date range.");
        }
    }
}

