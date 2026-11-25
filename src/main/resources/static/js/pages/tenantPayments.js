/**
 * Tenant Payment History Module
 */

import { api } from '../api.js';
import { renderTenantLayout, bindLayoutEvents } from '../layout.js';
import { renderSkeletonTable } from '../ui/skeleton.js';
import { renderEmptyState } from '../ui/empty.js';
import { escapeHtml, formatCurrency, formatDateTime } from '../ui/form.js';
import { toast } from '../ui/toast.js';

let currentPage = 0;

export async function renderTenantPayments() {
    const appEl = document.getElementById('app');
    appEl.innerHTML = renderTenantLayout(`
        <div class="mb-4">
            <h4 class="fw-bold mb-1">Payment Transactions</h4>
            <p class="text-muted mb-0 small">Official ledger of all verified rent and utility payments</p>
        </div>

        <div id="paymentsContainer">
            ${renderSkeletonTable(6, 5)}
        </div>
    `, 'payments');
    bindLayoutEvents();

    await loadPayments(0);
}

async function loadPayments(page = 0) {
    currentPage = page;
    const container = document.getElementById('paymentsContainer');
    if (!container) return;

    try {
        const res = await api.get('/api/v1/payments/my', { page, size: 10, sort: 'paidAt,desc' });
        const payments = res.content || [];

        if (payments.length === 0) {
            container.innerHTML = renderEmptyState({
                title: 'No Payments Recorded',
                description: 'You have not submitted any payments yet.',
                actionText: 'View Bills',
                actionHash: '#/tenant/bills'
            });
            return;
        }

        container.innerHTML = `
            <div class="card p-3">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th>Transaction Ref</th>
                                <th>Invoice Statement</th>
                                <th>Date &amp; Time</th>
                                <th>Payment Method</th>
                                <th class="text-end">Amount Paid</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${payments.map(p => `
                                <tr>
                                    <td class="font-monospace fw-semibold">${escapeHtml(p.transactionReference)}</td>
                                    <td>
                                        <a href="#/invoices/${p.invoiceId}" class="badge bg-light text-dark border text-decoration-none">
                                            #INV-${p.invoiceId}
                                        </a>
                                    </td>
                                    <td>${formatDateTime(p.paidAt)}</td>
                                    <td>
                                        <span class="badge bg-secondary-subtle text-secondary border">
                                            ${escapeHtml(p.paymentMethod)}
                                        </span>
                                    </td>
                                    <td class="text-end fw-bold text-success tabular-nums">
                                        ${formatCurrency(p.amountPaid)}
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>

                ${res.totalPages > 1 ? `
                    <div class="d-flex justify-content-between align-items-center mt-3 pt-3 border-top">
                        <div class="small text-muted">Page ${res.page + 1} of ${res.totalPages}</div>
                        <ul class="pagination pagination-sm mb-0">
                            <li class="page-item ${res.page === 0 ? 'disabled' : ''}">
                                <button class="page-link" id="prevPaymentsBtn">Previous</button>
                            </li>
                            <li class="page-item ${res.page >= res.totalPages - 1 ? 'disabled' : ''}">
                                <button class="page-link" id="nextPaymentsBtn">Next</button>
                            </li>
                        </ul>
                    </div>
                ` : ''}
            </div>
        `;

        const prevBtn = document.getElementById('prevPaymentsBtn');
        const nextBtn = document.getElementById('nextPaymentsBtn');
        if (prevBtn) prevBtn.onclick = () => loadPayments(currentPage - 1);
        if (nextBtn) nextBtn.onclick = () => loadPayments(currentPage + 1);

    } catch (err) {
        toast.error('Failed to load payments: ' + err.message);
    }
}

