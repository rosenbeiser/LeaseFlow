/**
 * Tenant Bills & Expandable Invoices Module
 */

import { api } from '../api.js';
import { renderTenantLayout, bindLayoutEvents } from '../layout.js';
import { renderSkeletonTable } from '../ui/skeleton.js';
import { renderEmptyState } from '../ui/empty.js';
import { renderInvoiceBadge } from '../ui/badge.js';
import { escapeHtml, formatCurrency, formatDate } from '../ui/form.js';
import { toast } from '../ui/toast.js';

let currentStatus = '';

export async function renderTenantBills() {
    const appEl = document.getElementById('app');
    appEl.innerHTML = renderTenantLayout(`
        <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">My Bills &amp; Invoices</h4>
                <p class="text-muted mb-0 small">Itemized monthly rent, metered utilities, and instant online payment</p>
            </div>
            <div class="d-flex gap-2">
                <select class="form-select form-select-sm" id="tenantStatusFilter" style="width: auto;">
                    <option value="">All Statuses</option>
                    <option value="PENDING">Pending</option>
                    <option value="PARTIALLY_PAID">Partially Paid</option>
                    <option value="OVERDUE">Overdue</option>
                    <option value="PAID">Paid</option>
                </select>
            </div>
        </div>

        <div id="tenantBillsContainer">
            ${renderSkeletonTable(5, 6)}
        </div>

        <!-- Pay Invoice Modal -->
        <div class="modal fade" id="tenantPayModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow-lg">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold">Pay Invoice Statement</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <form id="tenantPayForm">
                        <input type="hidden" id="payModalInvoiceId">
                        <div class="modal-body p-4">
                            <div class="alert alert-info py-2 px-3 small mb-3">
                                <span id="payModalInvoiceTitle">Invoice Payment</span><br>
                                Outstanding balance: <strong id="payModalBalance" class="tabular-nums">$0.00</strong>
                            </div>

                            <div class="mb-3">
                                <label class="form-label fw-semibold">Payment Amount ($)</label>
                                <input type="number" step="0.01" min="0.01" class="form-control tabular-nums" id="payModalAmount" required>
                                <div class="form-text small">You may make a partial payment or clear the entire balance.</div>
                            </div>

                            <div class="mb-3">
                                <label class="form-label fw-semibold">Payment Method</label>
                                <select class="form-select" id="payModalMethod" required>
                                    <option value="CARD">Credit / Debit Card</option>
                                    <option value="BANK_TRANSFER">Bank Transfer (ACH)</option>
                                    <option value="MOBILE_WALLET">Mobile Wallet</option>
                                </select>
                            </div>

                            <div class="mb-3">
                                <label class="form-label fw-semibold">Reference Number</label>
                                <input type="text" class="form-control font-monospace" id="payModalTxRef" required>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="submit" class="btn btn-success fw-semibold" id="confirmPayBtn">Submit Payment</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    `, 'bills');
    bindLayoutEvents();

    initFilter();
    initPayModal();
    await loadBills();
}

function initFilter() {
    const filterSelect = document.getElementById('tenantStatusFilter');
    if (filterSelect) {
        filterSelect.onchange = () => {
            currentStatus = filterSelect.value;
            loadBills();
        };
    }
}

async function loadBills() {
    const container = document.getElementById('tenantBillsContainer');
    if (!container) return;

    try {
        const params = { size: 50 };
        if (currentStatus) params.status = currentStatus;

        const res = await api.get('/api/v1/invoices/my-bills', params);
        const bills = res.content || [];

        if (bills.length === 0) {
            container.innerHTML = renderEmptyState({
                title: 'No Bills Found',
                description: currentStatus ? `No statements matching status ${currentStatus}.` : 'You have no billing statements issued yet.'
            });
            return;
        }

        container.innerHTML = `
            <div class="card p-0">
                <div class="table-responsive">
                    <table class="table align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th style="width: 40px;"></th>
                                <th>Invoice #</th>
                                <th>Billing Month</th>
                                <th>Due Date</th>
                                <th>Total</th>
                                <th>Paid</th>
                                <th>Balance Due</th>
                                <th>Status</th>
                                <th class="text-end">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${bills.map(b => {
                                const outstanding = b.outstandingBalance || 0;
                                const isPaid = b.status === 'PAID';
                                return `
                                    <tr class="bill-row" id="bill-row-${b.id}">
                                        <td>
                                            <button class="btn btn-sm btn-link p-0 text-secondary toggle-expand-btn" data-target="#breakdown-${b.id}">
                                                <i class="bi bi-chevron-down"></i>
                                            </button>
                                        </td>
                                        <td class="font-monospace fw-semibold">#INV-${b.id}</td>
                                        <td><span class="badge bg-light text-dark border font-monospace">${escapeHtml(b.billingMonth)}</span></td>
                                        <td>${formatDate(b.dueDate)}</td>
                                        <td class="fw-bold tabular-nums">${formatCurrency(b.totalPayable)}</td>
                                        <td class="tabular-nums text-muted">${formatCurrency(b.amountPaid)}</td>
                                        <td class="fw-bold tabular-nums ${outstanding > 0 ? 'text-danger' : 'text-success'}">
                                            ${formatCurrency(outstanding)}
                                        </td>
                                        <td>${renderInvoiceBadge(b.status)}</td>
                                        <td class="text-end">
                                            <div class="d-inline-flex gap-1">
                                                ${!isPaid && outstanding > 0 ? `
                                                    <button class="btn btn-sm btn-success pay-now-btn" data-invoice-id="${b.id}" data-month="${b.billingMonth}" data-balance="${outstanding}">
                                                        <i class="bi bi-credit-card me-1"></i>Pay Now
                                                    </button>
                                                ` : ''}
                                                <a href="#/invoices/${b.id}" class="btn btn-sm btn-outline-secondary" title="View & Print Statement">
                                                    <i class="bi bi-eye"></i>
                                                </a>
                                            </div>
                                        </td>
                                    </tr>

                                    <!-- Collapsible Itemized Line Items Breakdown -->
                                    <tr class="collapse-row d-none" id="breakdown-${b.id}">
                                        <td colspan="9" class="p-0 bg-body-tertiary">
                                            <div class="p-3 border-bottom">
                                                <div class="small fw-bold text-uppercase text-muted mb-2">Itemized Statement Breakdown</div>
                                                <div class="table-responsive">
                                                    <table class="table table-sm bg-body table-bordered mb-0">
                                                        <thead class="small table-light">
                                                            <tr>
                                                                <th>Item</th>
                                                                <th class="text-center">Utility</th>
                                                                <th class="text-center">Units Consumed</th>
                                                                <th class="text-end">Amount</th>
                                                            </tr>
                                                        </thead>
                                                        <tbody class="small">
                                                            ${(b.lineItems || []).map(li => `
                                                                <tr>
                                                                    <td>${escapeHtml(li.description)}</td>
                                                                    <td class="text-center">${li.utilityType ? `<span class="badge bg-secondary-subtle text-secondary">${li.utilityType}</span>` : '—'}</td>
                                                                    <td class="text-center tabular-nums">${li.unitsConsumed !== null && li.unitsConsumed !== undefined ? `${li.unitsConsumed} units` : '—'}</td>
                                                                    <td class="text-end fw-semibold tabular-nums">${formatCurrency(li.amount)}</td>
                                                                </tr>
                                                            `).join('')}

                                                            ${b.fineAmount > 0 ? `
                                                                <tr class="table-danger">
                                                                    <td colspan="3" class="text-danger fw-semibold"><i class="bi bi-exclamation-triangle me-1"></i>Late Fine (2% overdue)</td>
                                                                    <td class="text-end fw-bold text-danger tabular-nums">+${formatCurrency(b.fineAmount)}</td>
                                                                </tr>
                                                            ` : ''}
                                                        </tbody>
                                                    </table>
                                                </div>
                                            </div>
                                        </td>
                                    </tr>
                                `;
                            }).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;

        // Bind expand/collapse rows
        container.querySelectorAll('.toggle-expand-btn').forEach(btn => {
            btn.onclick = () => {
                const targetSelector = btn.getAttribute('data-target');
                const row = document.querySelector(targetSelector);
                if (row) {
                    const isHidden = row.classList.contains('d-none');
                    row.classList.toggle('d-none', !isHidden);
                    btn.innerHTML = `<i class="bi bi-chevron-${isHidden ? 'up' : 'down'}"></i>`;
                }
            };
        });

        // Bind Pay Now buttons
        container.querySelectorAll('.pay-now-btn').forEach(btn => {
            btn.onclick = () => {
                const invoiceId = btn.getAttribute('data-invoice-id');
                const month = btn.getAttribute('data-month');
                const balance = parseFloat(btn.getAttribute('data-balance'));
                openPayDialog(invoiceId, month, balance);
            };
        });

    } catch (err) {
        toast.error('Failed to load bills: ' + err.message);
    }
}

function openPayDialog(invoiceId, month, balance) {
    const modalEl = document.getElementById('tenantPayModal');
    const idInput = document.getElementById('payModalInvoiceId');
    const titleEl = document.getElementById('payModalInvoiceTitle');
    const balEl = document.getElementById('payModalBalance');
    const amountInput = document.getElementById('payModalAmount');
    const txInput = document.getElementById('payModalTxRef');

    if (!modalEl) return;
    const bsModal = new window.bootstrap.Modal(modalEl);

    idInput.value = invoiceId;
    titleEl.textContent = `Statement #INV-${invoiceId} (${month})`;
    balEl.textContent = formatCurrency(balance);
    amountInput.value = balance.toFixed(2);
    amountInput.max = balance;
    txInput.value = 'TXN-' + Math.random().toString(36).substring(2, 10).toUpperCase();

    bsModal.show();
}

function initPayModal() {
    const modalEl = document.getElementById('tenantPayModal');
    const form = document.getElementById('tenantPayForm');
    const idInput = document.getElementById('payModalInvoiceId');
    const amountInput = document.getElementById('payModalAmount');
    const methodSelect = document.getElementById('payModalMethod');
    const txInput = document.getElementById('payModalTxRef');
    const confirmBtn = document.getElementById('confirmPayBtn');

    if (!modalEl || !form) return;
    const bsModal = window.bootstrap.Modal.getInstance(modalEl) || new window.bootstrap.Modal(modalEl);

    form.onsubmit = async (e) => {
        e.preventDefault();
        const invoiceId = idInput.value;
        const amount = Number(amountInput.value);
        const method = methodSelect.value;
        const txRef = txInput.value.trim();

        if (amount <= 0) {
            toast.warning('Payment amount must be greater than zero.');
            return;
        }

        confirmBtn.disabled = true;
        confirmBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Processing...';

        try {
            await api.post(`/api/v1/invoices/${invoiceId}/pay`, {
                amount,
                paymentMethod: method,
                transactionReference: txRef
            });

            toast.success(`Payment of ${formatCurrency(amount)} recorded successfully!`);
            const modalInstance = window.bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) modalInstance.hide();
            await loadBills();
        } catch (err) {
            toast.error(err.message || 'Payment submission failed.');
        } finally {
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = 'Submit Payment';
        }
    };
}

