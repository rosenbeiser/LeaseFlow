package com.leaseflow.pattern.state;

import com.leaseflow.entity.Lease;
import com.leaseflow.enums.LeaseStatus;

/**
 * Factory that resolves the correct {@link LeaseState} implementation from a
 * {@link Lease}'s current {@link LeaseStatus}. Used by service methods to obtain
 * the state object before delegating an operation to it.
 *
 * <p>Keeping the factory here (rather than on the entity) maintains a clean
 * separation: entities are pure data holders; behaviour lives in the state classes.
 */
public class LeaseStateFactory {

    private LeaseStateFactory() { /* utility */ }

    public static LeaseState getState(Lease lease) {
        return switch (lease.getStatus()) {
            case DRAFT         -> new DraftState();
            case ACTIVE        -> new ActiveState();
            case NOTICE_SERVED -> new NoticeServedState();
            case TERMINATED    -> new TerminatedState();
            case SETTLED       -> new SettledState();
        };
    }

    public static LeaseState getState(LeaseStatus status) {
        return switch (status) {
            case DRAFT         -> new DraftState();
            case ACTIVE        -> new ActiveState();
            case NOTICE_SERVED -> new NoticeServedState();
            case TERMINATED    -> new TerminatedState();
            case SETTLED       -> new SettledState();
        };
    }
}

