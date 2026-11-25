package com.leaseflow.pattern.state;

import com.leaseflow.entity.Lease;
import com.leaseflow.entity.User;
import com.leaseflow.exception.InvalidStateTransitionException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Concrete state for a lease in {@code SETTLED} status — a terminal state.
 *
 * <p>No further transitions are permitted. All operations throw
 * {@link InvalidStateTransitionException}. The lease is effectively archived.
 */
public class SettledState implements LeaseState {

    private static final String MSG = "Lease is already SETTLED. No further actions are permitted.";

    @Override
    public Lease activate(Lease lease, User actor) {
        throw new InvalidStateTransitionException(MSG);
    }

    @Override
    public Lease serveNotice(Lease lease, User actor) {
        throw new InvalidStateTransitionException(MSG);
    }

    @Override
    public Lease terminate(Lease lease, User actor) {
        throw new InvalidStateTransitionException(MSG);
    }

    @Override
    public Lease generateInvoice(Lease lease, User actor) {
        throw new InvalidStateTransitionException(
                "Cannot generate an invoice for a SETTLED lease.");
    }

    @Override
    public Lease settle(Lease lease, User actor, BigDecimal damagesAmount, String notes) {
        throw new InvalidStateTransitionException(MSG);
    }

    @Override
    public List<String> getAllowedActions() {
        return List.of();
    }
}

