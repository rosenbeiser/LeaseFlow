/**
 * Status Badge Component Helper
 */

export function renderLeaseBadge(status) {
    const s = status || 'DRAFT';
    const labels = {
        'DRAFT': 'Draft',
        'ACTIVE': 'Active',
        'NOTICE_SERVED': 'Notice Served',
        'TERMINATED': 'Terminated',
        'SETTLED': 'Settled'
    };
    const label = labels[s] || s;
    return `<span class="badge-status badge-lease-${s}">${label}</span>`;
}

export function renderInvoiceBadge(status) {
    const s = status || 'PENDING';
    const labels = {
        'PENDING': 'Pending',
        'PARTIALLY_PAID': 'Partially Paid',
        'PAID': 'Paid',
        'OVERDUE': 'Overdue'
    };
    const label = labels[s] || s;
    return `<span class="badge-status badge-invoice-${s}">${label}</span>`;
}

export function renderUnitBadge(status) {
    const s = status || 'VACANT';
    const label = s === 'OCCUPIED' ? 'Occupied' : 'Vacant';
    return `<span class="badge-status badge-unit-${s}">${label}</span>`;
}

