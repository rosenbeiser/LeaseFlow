package com.leaseflow.pattern.state;

import com.leaseflow.entity.Lease;
import com.leaseflow.entity.User;

import java.math.BigDecimal;
import java.util.List;

/**
 * <b>State Pattern — Context Interface</b>
 *
 * <p>Defines all operations that can be performed on a {@link Lease}. Each concrete
 * state class ({@code DraftState}, {@code ActiveState}, etc.) implements this interface
 * and either performs the transition or throws {@code InvalidStateTransitionException}
 * for disallowed operations in that state.
 *
 * <h2>Why State Pattern?</h2>
 * <p>The lease lifecycle has strict, non-trivial rules about which actions are valid in
 * each state. Without this pattern the service methods would be riddled with
 * {@code if/else} chains on {@code lease.getStatus()}, making it easy to miss an
 * illegal transition. Encapsulating behaviour in state objects makes each state
 * self-documenting, independently testable, and impossible to bypass accidentally.
 *
 * <h2>Lease Lifecycle State Machine</h2>
 * <pre>
 *   DRAFT ──activate()──► ACTIVE ──serveNotice()──► NOTICE_SERVED
 *                            │                           │
 *                            └────terminate()────────────┘
 *                                        │
 *                                   TERMINATED
 *                                        │
 *                                   settle()
 *                                        │
 *                                    SETTLED
 * </pre>
 *
 * <p>The context ({@code LeaseStateContext}) holds a reference to the current
 * {@code LeaseState} and delegates all operations to it. Transitions are written to
 * {@code LeaseStatusHistory} inside each concrete state's transition method.
 *
 * @author Student 2207083
 */
public interface LeaseState {

    /**
     * Transition DRAFT → ACTIVE. Only valid in {@code DraftState}.
     * Side-effects: marks the unit OCCUPIED, writes history row.
     *
     * @param lease the lease to activate
     * @param actor the user performing the action (must be the landlord or admin)
     * @return the updated lease with status ACTIVE
     */
    Lease activate(Lease lease, User actor);

    /**
     * Transition ACTIVE → NOTICE_SERVED. Only valid in {@code ActiveState}.
     * Side-effects: writes history row.
     *
     * @param lease the active lease
     * @param actor the user performing the action
     * @return the updated lease with status NOTICE_SERVED
     */
    Lease serveNotice(Lease lease, User actor);

    /**
     * Transition ACTIVE or NOTICE_SERVED → TERMINATED. Only valid in those two states.
     * Side-effects: marks the unit VACANT, writes history row.
     *
     * @param lease the lease to terminate
     * @param actor the user performing the action
     * @return the updated lease with status TERMINATED
     */
    Lease terminate(Lease lease, User actor);

    /**
     * Generate a monthly invoice. Only valid in ACTIVE and NOTICE_SERVED states.
     * Delegates billing calculation to the Strategy Pattern via {@code InvoiceService}.
     *
     * @param lease the lease to invoice
     * @param actor the user performing the action
     * @return the updated lease (invoice is persisted as a side-effect)
     */
    Lease generateInvoice(Lease lease, User actor);

    /**
     * Transition TERMINATED → SETTLED. Only valid in {@code TerminatedState}.
     * Computes: {@code refund = securityDeposit - unpaidBalance - damagesAmount}.
     * Side-effects: writes history row with notes.
     *
     * @param lease         the terminated lease
     * @param actor         the user performing the action
     * @param damagesAmount any damages to deduct from the security deposit
     * @param notes         settlement notes (required, written to history)
     * @return the updated lease with status SETTLED
     */
    Lease settle(Lease lease, User actor, BigDecimal damagesAmount, String notes);

    /**
     * Returns the list of action names that are currently permitted in this state.
     * Used by the API to drive the UI's action buttons — the frontend renders ONLY
     * the buttons returned here, preventing invalid action attempts without trusting
     * the client for security (the backend still validates every request).
     *
     * @return immutable list of allowed action name strings
     */
    List<String> getAllowedActions();
}
