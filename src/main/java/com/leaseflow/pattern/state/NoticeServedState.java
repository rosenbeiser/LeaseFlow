package com.leaseflow.pattern.state;

import com.leaseflow.entity.Lease;
import com.leaseflow.entity.LeaseStatusHistory;
import com.leaseflow.entity.User;
import com.leaseflow.enums.LeaseStatus;
import com.leaseflow.enums.UnitStatus;
import com.leaseflow.exception.InvalidStateTransitionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Concrete state for a lease in {@code NOTICE_SERVED} status.
 *
 * <p>A notice period is running: final invoices may still be generated, and the
 * lease must be formally terminated before settlement can occur.
 *
 * <p>Allowed transitions:
 * <ul>
 *   <li>{@link #terminate}       → TERMINATED (marks unit VACANT)</li>
 *   <li>{@link #generateInvoice} — permitted (no status change)</li>
 * </ul>
 */
public class NoticeServedState implements LeaseState {

    @Override
    public Lease activate(Lease lease, User actor) {
        throw new InvalidStateTransitionException(
                "Lease is already NOTICE_SERVED and cannot be re-activated.");
    }

    @Override
    public Lease serveNotice(Lease lease, User actor) {
        throw new InvalidStateTransitionException(
                "Notice has already been served on this lease.");
    }

    @Override
    public Lease terminate(Lease lease, User actor) {
        lease.setStatus(LeaseStatus.TERMINATED);
        lease.getUnit().setStatus(UnitStatus.VACANT);
        writeHistory(lease, LeaseStatus.NOTICE_SERVED, LeaseStatus.TERMINATED, actor, null);
        return lease;
    }

    @Override
    public Lease generateInvoice(Lease lease, User actor) {
        // Permitted during notice period; no state change. Service handles creation.
        return lease;
    }

    @Override
    public Lease settle(Lease lease, User actor, BigDecimal damagesAmount, String notes) {
        throw new InvalidStateTransitionException(
                "Cannot settle a NOTICE_SERVED lease. Terminate it first.");
    }

    @Override
    public List<String> getAllowedActions() {
        return List.of("TERMINATE", "GENERATE_INVOICE");
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

