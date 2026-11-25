package com.leaseflow.pattern.state;

import com.leaseflow.entity.Lease;
import com.leaseflow.entity.LeaseStatusHistory;
import com.leaseflow.entity.User;
import com.leaseflow.enums.LeaseStatus;
import com.leaseflow.exception.InvalidStateTransitionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Concrete state for a lease in {@code TERMINATED} status.
 *
 * <p>The only remaining action is {@link #settle}, which computes the security
 * deposit refund, records any damage deductions, and moves the lease to SETTLED.
 *
 * <p>Formula: {@code refund = securityDeposit - unpaidInvoiceBalance - damagesAmount}.
 * A negative result means the tenant owes the difference.
 */
public class TerminatedState implements LeaseState {

    @Override
    public Lease activate(Lease lease, User actor) {
        throw new InvalidStateTransitionException(
                "Cannot activate a TERMINATED lease.");
    }

    @Override
    public Lease serveNotice(Lease lease, User actor) {
        throw new InvalidStateTransitionException(
                "Cannot serve notice on a TERMINATED lease.");
    }

    @Override
    public Lease terminate(Lease lease, User actor) {
        throw new InvalidStateTransitionException(
                "Lease is already TERMINATED.");
    }

    @Override
    public Lease generateInvoice(Lease lease, User actor) {
        throw new InvalidStateTransitionException(
                "Cannot generate an invoice for a TERMINATED lease.");
    }

    @Override
    public Lease settle(Lease lease, User actor, BigDecimal damagesAmount, String notes) {
        lease.setStatus(LeaseStatus.SETTLED);
        String historyNote = buildNote(damagesAmount, notes);
        writeHistory(lease, LeaseStatus.TERMINATED, LeaseStatus.SETTLED, actor, historyNote);
        return lease;
    }

    @Override
    public List<String> getAllowedActions() {
        return List.of("SETTLE");
    }

    private String buildNote(BigDecimal damagesAmount, String notes) {
        StringBuilder sb = new StringBuilder();
        if (damagesAmount != null && damagesAmount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Damages deducted: ").append(damagesAmount);
        }
        if (notes != null && !notes.isBlank()) {
            if (!sb.isEmpty()) sb.append(". ");
            sb.append(notes);
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private void writeHistory(Lease lease, LeaseStatus from, LeaseStatus to, User actor, String notes) {
        LeaseStatusHistory history = LeaseStatusHistory.builder()
                .lease(lease)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(actor.getId())
                .changedAt(LocalDateTime.now())
                .notes(notes)
                .build();
        lease.getStatusHistory().add(history);
    }
}

