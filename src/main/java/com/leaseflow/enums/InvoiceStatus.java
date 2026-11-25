package com.leaseflow.enums;

/**
 * Payment/billing status of an Invoice.
 * Transitions driven by payment receipts and the scheduled late-fine job:
 *   PENDING -> PARTIALLY_PAID (partial payment received)
 *   PENDING / PARTIALLY_PAID -> PAID (full balance cleared)
 *   PENDING -> OVERDUE (scheduled job after dueDate passes)
 */
public enum InvoiceStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID,
    OVERDUE
}

