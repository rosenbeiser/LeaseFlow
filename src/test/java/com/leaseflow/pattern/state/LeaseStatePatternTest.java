package com.leaseflow.pattern.state;

import com.leaseflow.entity.Lease;
import com.leaseflow.entity.Unit;
import com.leaseflow.entity.User;
import com.leaseflow.enums.LeaseStatus;
import com.leaseflow.enums.Role;
import com.leaseflow.enums.UnitStatus;
import com.leaseflow.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Lease State Pattern Lifecycle Tests")
class LeaseStatePatternTest {

    private User actor;
    private Unit unit;
    private Lease lease;

    @BeforeEach
    void setUp() {
        actor = User.builder()
                .id(1L)
                .email("landlord@leaseflow.dev")
                .fullName("Landlord Actor")
                .role(Role.ROLE_LANDLORD)
                .build();

        unit = Unit.builder()
                .id(10L)
                .unitNumber("101")
                .status(UnitStatus.VACANT)
                .build();

        lease = Lease.builder()
                .id(100L)
                .unit(unit)
                .tenant(User.builder().id(2L).role(Role.ROLE_TENANT).build())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(12))
                .baseRent(new BigDecimal("1500.00"))
                .securityDeposit(new BigDecimal("3000.00"))
                .status(LeaseStatus.DRAFT)
                .statusHistory(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Draft state allows activation and marks unit OCCUPIED, disallowing illegal actions")
    void testDraftState() {
        LeaseState state = LeaseStateFactory.getState(lease);
        assertEquals(LeaseStatus.DRAFT, lease.getStatus());
        assertTrue(state.getAllowedActions().contains("ACTIVATE"));
        assertEquals(1, state.getAllowedActions().size());

        // Illegal transitions from DRAFT
        assertThrows(InvalidStateTransitionException.class, () -> state.serveNotice(lease, actor));
        assertThrows(InvalidStateTransitionException.class, () -> state.terminate(lease, actor));
        assertThrows(InvalidStateTransitionException.class, () -> state.generateInvoice(lease, actor));
        assertThrows(InvalidStateTransitionException.class, () -> state.settle(lease, actor, BigDecimal.ZERO, ""));

        // Valid activation
        state.activate(lease, actor);
        assertEquals(LeaseStatus.ACTIVE, lease.getStatus());
        assertEquals(UnitStatus.OCCUPIED, unit.getStatus());
        assertEquals(1, lease.getStatusHistory().size());
        assertEquals(LeaseStatus.DRAFT, lease.getStatusHistory().get(0).getFromStatus());
        assertEquals(LeaseStatus.ACTIVE, lease.getStatusHistory().get(0).getToStatus());
    }

    @Test
    @DisplayName("Active state allows notice, invoice generation, early termination, and disallows settle/activate")
    void testActiveState() {
        // Move to ACTIVE
        LeaseStateFactory.getState(lease).activate(lease, actor);
        LeaseState state = LeaseStateFactory.getState(lease);

        assertTrue(state.getAllowedActions().contains("SERVE_NOTICE"));
        assertTrue(state.getAllowedActions().contains("TERMINATE"));
        assertTrue(state.getAllowedActions().contains("GENERATE_INVOICE"));

        // Illegal in ACTIVE
        assertThrows(InvalidStateTransitionException.class, () -> state.activate(lease, actor));
        assertThrows(InvalidStateTransitionException.class, () -> state.settle(lease, actor, BigDecimal.ZERO, ""));

        // Invoicing permitted without status change
        assertDoesNotThrow(() -> state.generateInvoice(lease, actor));
        assertEquals(LeaseStatus.ACTIVE, lease.getStatus());

        // Serve notice transition
        state.serveNotice(lease, actor);
        assertEquals(LeaseStatus.NOTICE_SERVED, lease.getStatus());
        assertEquals(2, lease.getStatusHistory().size());
    }

    @Test
    @DisplayName("Active state allows direct early termination")
    void testActiveDirectTermination() {
        LeaseStateFactory.getState(lease).activate(lease, actor);
        LeaseState state = LeaseStateFactory.getState(lease);

        state.terminate(lease, actor);
        assertEquals(LeaseStatus.TERMINATED, lease.getStatus());
        assertEquals(UnitStatus.VACANT, unit.getStatus());
    }

    @Test
    @DisplayName("NoticeServed state allows invoice generation and termination, rejects reactivate/settle")
    void testNoticeServedState() {
        LeaseStateFactory.getState(lease).activate(lease, actor);
        LeaseStateFactory.getState(lease).serveNotice(lease, actor);
        LeaseState state = LeaseStateFactory.getState(lease);

        assertTrue(state.getAllowedActions().contains("TERMINATE"));
        assertTrue(state.getAllowedActions().contains("GENERATE_INVOICE"));

        // Illegal in NOTICE_SERVED
        assertThrows(InvalidStateTransitionException.class, () -> state.activate(lease, actor));
        assertThrows(InvalidStateTransitionException.class, () -> state.serveNotice(lease, actor));
        assertThrows(InvalidStateTransitionException.class, () -> state.settle(lease, actor, BigDecimal.ZERO, ""));

        // Permitted
        assertDoesNotThrow(() -> state.generateInvoice(lease, actor));

        // Terminate
        state.terminate(lease, actor);
        assertEquals(LeaseStatus.TERMINATED, lease.getStatus());
        assertEquals(UnitStatus.VACANT, unit.getStatus());
    }

    @Test
    @DisplayName("Terminated state allows settlement and marks status SETTLED, disallowing all other operations")
    void testTerminatedState() {
        LeaseStateFactory.getState(lease).activate(lease, actor);
        LeaseStateFactory.getState(lease).terminate(lease, actor);
        LeaseState state = LeaseStateFactory.getState(lease);

        assertEquals(List.of("SETTLE"), state.getAllowedActions());

        // Illegal in TERMINATED
        assertThrows(InvalidStateTransitionException.class, () -> state.activate(lease, actor));
        assertThrows(InvalidStateTransitionException.class, () -> state.serveNotice(lease, actor));
        assertThrows(InvalidStateTransitionException.class, () -> state.terminate(lease, actor));
        assertThrows(InvalidStateTransitionException.class, () -> state.generateInvoice(lease, actor));

        // Valid settle
        state.settle(lease, actor, new BigDecimal("250.00"), "Wall repaint deduction");
        assertEquals(LeaseStatus.SETTLED, lease.getStatus());
        assertTrue(lease.getStatusHistory().get(lease.getStatusHistory().size() - 1).getNotes().contains("Wall repaint"));
    }

    @Test
    @DisplayName("Settled state is terminal with zero allowed actions")
    void testSettledStateTerminal() {
        LeaseStateFactory.getState(lease).activate(lease, actor);
        LeaseStateFactory.getState(lease).terminate(lease, actor);
        LeaseStateFactory.getState(lease).settle(lease, actor, BigDecimal.ZERO, "Full return");

        LeaseState state = LeaseStateFactory.getState(lease);
        assertTrue(state.getAllowedActions().isEmpty());

        assertThrows(InvalidStateTransitionException.class, () -> state.activate(lease, actor));
        assertThrows(InvalidStateTransitionException.class, () -> state.serveNotice(lease, actor));
        assertThrows(InvalidStateTransitionException.class, () -> state.terminate(lease, actor));
        assertThrows(InvalidStateTransitionException.class, () -> state.generateInvoice(lease, actor));
        assertThrows(InvalidStateTransitionException.class, () -> state.settle(lease, actor, BigDecimal.ZERO, ""));
    }
}

