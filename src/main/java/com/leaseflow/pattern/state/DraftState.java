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
 * Concrete state for a lease in {@code DRAFT} status.
 *
 * <p>Allowed transitions:
 * <ul>
 *   <li>{@link #activate} → ACTIVE (marks unit OCCUPIED, writes history)</li>
 * </ul>
 * All other operations throw {@link InvalidStateTransitionException}.
 */
public class DraftState implements LeaseState {

    @Override
    public Lease activate(Lease lease, User actor) {
        lease.setStatus(LeaseStatus.ACTIVE);
        lease.getUnit().setStatus(UnitStatus.OCCUPIED);
        writeHistory(lease, LeaseStatus.DRAFT, LeaseStatus.ACTIVE, actor, null);
        return lease;
    }

    @Override
    public Lease serveNotice(Lease lease, User actor) {
        throw new InvalidStateTransitionException(
                "Cannot serve notice on a DRAFT lease. Activate it first.");
    }

    @Override
    public Lease terminate(Lease lease, User actor) {
        throw new InvalidStateTransitionException(
                "Cannot terminate a DRAFT lease. Activate it first.");
    }

    @Override
    public Lease generateInvoice(Lease lease, User actor) {
        throw new InvalidStateTransitionException(
                "Cannot generate an invoice for a DRAFT lease. Only ACTIVE or NOTICE_SERVED leases can be invoiced.");
    }

    @Override
    public Lease settle(Lease lease, User actor, BigDecimal damagesAmount, String notes) {
        throw new InvalidStateTransitionException(
                "Cannot settle a DRAFT lease. The lease must be TERMINATED first.");
    }

    @Override
    public List<String> getAllowedActions() {
        return List.of("ACTIVATE");
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

