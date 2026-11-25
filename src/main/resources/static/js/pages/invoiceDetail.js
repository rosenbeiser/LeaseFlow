/**
 * Invoice Detail & Itemized Billing View Module
 */

import { api } from '../api.js';
import { auth } from '../auth.js';
import { renderLandlordLayout, renderTenantLayout, bindLayoutEvents } from '../layout.js';
import { renderInvoiceBadge } from '../ui/badge.js';
import { escapeHtml, formatCurrency, formatDate, formatDateTime } from '../ui/form.js';
import { toast } from '../ui/toast.js';

export async function renderInvoiceDetail(params) {
    const invoiceId = params.id;
    const isLandlord = auth.isLandlord() || auth.isAdmin();
    const renderLayout = isLandlord ? renderLandlordLayout : renderTenantLayout;

    const appEl = document.getElementById('app');
    appEl.innerHTML = renderLayout(`
        <div class="mb-4">
            <div class="d-flex justify-content-between align-items-center mb-3 no-print">
                <a href="${isLandlord ? '#/landlord/invoices' : '#/tenant/bills'}" class="btn btn-sm btn-outline-secondary">
                    <i class="bi bi-arrow-left me-1"></i>Back to Invoices
                </a>
                <button class="btn btn-sm btn-outline-dark" onclick="window.print()">
                    <i class="bi bi-printer me-1"></i>Print Statement
                </button>
            </div>

            <div id="invoiceDetailContainer">
                <div class="card p-5 text-center">
                    <div class="spinner-border text-primary mx-auto mb-2" role="status"></div>
                    <div class="text-muted">Loading invoice statement...</div>
                </div>
            </div>
        </div>

        <!-- Tenant Pay Modal -->
        <div class="modal fade" id="payInvoiceModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow-lg">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold">Pay Invoice Statement</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <form id="payInvoiceForm">
                        <div class="modal-body p-4">
                            <div class="alert alert-primary py-2 px-3 small mb-3">
                                Outstanding balance: <strong id="modalOutstandingDisplay" class="tabular-nums">$0.00</strong>
                            </div>

                            <div class="mb-3">
                                <label class="form-label fw-semibold">Payment Amount ($)</label>
                                <input type="number" step="0.01" min="0.01" class="form-control tabular-nums" id="paymentAmountInput" required>
                                <div class="form-text small">Cannot exceed total outstanding balance.</div>
                            </div>

                            <div class="mb-3">
                                <label class="form-label fw-semibold">Payment Method</label>
                                <select class="form-select" id="paymentMethodSelect" required>
                                    <option value="CARD">Credit / Debit Card</option>
                                    <option value="BANK_TRANSFER">Bank Transfer (ACH)</option>
                                    <option value="MOBILE_WALLET">Mobile Wallet</option>
                                    <option value="CASH">Cash</option>
                                </select>
                            </div>

                            <div class="mb-3">
                                <label class="form-label fw-semibold">Transaction Reference / Note</label>
                                <input type="text" class="form-control font-monospace" id="paymentTxRefInput" required placeholder="e.g. TXN-12345678">
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="submit" class="btn btn-success fw-semibold" id="executePayBtn">Confirm Payment</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    `, isLandlord ? 'invoices' : 'bills');
    bindLayoutEvents();

    await loadInvoiceDetail(invoiceId, isLandlord);
}

async function loadInvoiceDetail(invoiceId, isLandlord) {
    const container = document.getElementById('invoiceDetailContainer');
    if (!container) return;

    try {
        const inv = await api.get(`/api/v1/invoices/${invoiceId}`);
        const outstanding = inv.outstandingBalance || 0;
        const isPaid = inv.status === 'PAID';

        container.innerHTML = `
            <div class="card p-4 p-md-5 print-invoice-sheet">
                <!-- Header Banner -->
                <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-start pb-4 border-bottom mb-4 gap-3">
                    <div>
                        <div class="d-flex align-items-center gap-2 mb-2">
                            <i class="bi bi-building-gear fs-2 text-primary"></i>
                            <h3 class="fw-bold mb-0 letter-spacing-tight">LeaseFlow</h3>
                        </div>
                        <div class="text-muted small">Smart Property Lease &amp; Utility Billing Management</div>
                    </div>
                    <div class="text-sm-end">
                        <div class="d-flex align-items-center justify-content-sm-end gap-2 mb-1">
                            <h4 class="fw-bold mb-0">INVOICE</h4>
                            ${renderInvoiceBadge(inv.status)}
                        </div>
                        <div class="font-monospace text-secondary fw-semibold">#INV-${inv.id}</div>
                        <div class="small text-muted">Billing Period: <strong>${escapeHtml(inv.billingMonth)}</strong></div>
                    </div>
                </div>

                <!-- Parties & Dates Info -->
                <div class="row g-4 mb-4 pb-3 border-bottom">
                    <div class="col-sm-6">
                        <div class="text-uppercase small text-muted fw-bold mb-1">Billed To</div>
                        <div class="fw-bold fs-6">${escapeHtml(inv.tenantName)}</div>
                        <div class="text-secondary small">Tenant ID: #${inv.tenantId}</div>
                        <div class="text-secondary small">${escapeHtml(inv.propertyName)} — Unit ${escapeHtml(inv.unitNumber)}</div>
                    </div>
                    <div class="col-sm-6 text-sm-end">
                        <div class="row g-2">
                            <div class="col-6 text-sm-start small text-muted">Invoice Date:</div>
                            <div class="col-6 text-sm-end small fw-semibold">${formatDate(inv.createdAt)}</div>
                            <div class="col-6 text-sm-start small text-muted">Due Date:</div>
                            <div class="col-6 text-sm-end small fw-bold text-danger">${formatDate(inv.dueDate)}</div>
                            <div class="col-6 text-sm-start small text-muted">Associated Lease:</div>
                            <div class="col-6 text-sm-end small">
                                <a href="#/leases/${inv.leaseId}" class="text-decoration-none">#LSE-${inv.leaseId}</a>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Line Items Table -->
                <div class="table-responsive mb-4">
                    <table class="table align-middle">
                        <thead class="table-light">
                            <tr>
                                <th>Item Description</th>
                                <th class="text-center">Units Metered</th>
                                <th class="text-end">Amount</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${(inv.lineItems || []).map(li => `
                                <tr>
                                    <td>
                                        <div class="fw-semibold">${escapeHtml(li.description)}</div>
                                        ${li.utilityType ? `<span class="badge bg-light text-dark border small">${li.utilityType}</span>` : ''}
                                    </td>
                                    <td class="text-center tabular-nums text-muted">
                                        ${li.unitsConsumed !== null && li.unitsConsumed !== undefined ? `${li.unitsConsumed} units` : '—'}
                                    </td>
                                    <td class="text-end fw-semibold tabular-nums">
                                        ${formatCurrency(li.amount)}
                                    </td>
                                </tr>
                            `).join('')}

                            ${inv.fineAmount > 0 ? `
                                <tr class="table-danger">
                                    <td>
                                        <div class="fw-semibold text-danger"><i class="bi bi-exclamation-circle me-1"></i>Late Payment Fine (2% overdue compounding)</div>
                                    </td>
                                    <td class="text-center text-muted">—</td>
                                    <td class="text-end fw-bold text-danger tabular-nums">+${formatCurrency(inv.fineAmount)}</td>
                                </tr>
                            ` : ''}
                        </tbody>
                        <tfoot>
                            <tr>
                                <th colspan="2" class="text-end pt-3">Total Payable:</th>
                                <th class="text-end pt-3 fs-5 fw-bold text-primary tabular-nums">${formatCurrency(inv.totalPayable)}</th>
                            </tr>
                            <tr>
                                <td colspan="2" class="text-end border-0 text-muted">Total Amount Paid:</td>
                                <td class="text-end border-0 tabular-nums text-success">${formatCurrency(inv.amountPaid)}</td>
                            </tr>
                            <tr class="table-light">
                                <th colspan="2" class="text-end border-top">Outstanding Balance:</th>
                                <th class="text-end border-top fs-5 fw-bold ${outstanding > 0 ? 'text-danger' : 'text-success'} tabular-nums">
                                    ${formatCurrency(outstanding)}
                                </th>
                            </tr>
                        </tfoot>
                    </table>
                </div>

                <!-- Payment Action & Payment Transactions Table -->
                <div class="mt-4 pt-3 border-top">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h6 class="fw-bold mb-0">Payment History</h6>
                        ${!isLandlord && !isPaid ? `
                            <button class="btn btn-success fw-semibold" id="openPayModalBtn">
                                <i class="bi bi-credit-card me-1"></i>Pay Now (${formatCurrency(outstanding)})
                            </button>
                        ` : ''}
                    </div>

                    ${(inv.payments || []).length === 0 ? `
                        <div class="p-3 rounded bg-body-tertiary text-muted small text-center">
                            No payment transactions recorded against this statement yet.
                        </div>
                    ` : `
                        <div class="table-responsive">
                            <table class="table table-sm table-hover align-middle mb-0">
                                <thead class="table-light small">
                                    <tr>
                                        <th>Date &amp; Time</th>
                                        <th>Transaction Ref</th>
                                        <th>Method</th>
                                        <th class="text-end">Amount Paid</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${inv.payments.map(p => `
                                        <tr>
                                            <td>${formatDateTime(p.paidAt)}</td>
                                            <td class="font-monospace text-secondary">${escapeHtml(p.transactionReference)}</td>
                                            <td><span class="badge bg-light text-dark border">${escapeHtml(p.paymentMethod)}</span></td>
                                            <td class="text-end fw-bold text-success tabular-nums">${formatCurrency(p.amountPaid)}</td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    `}
                </div>
            </div>
        `;

        initPayModal(inv, isLandlord);

    } catch (err) {
        container.innerHTML = `<div class="alert alert-danger">${escapeHtml(err.message)}</div>`;
    }
}

function initPayModal(inv, isLandlord) {
    if (isLandlord) return;

    const openBtn = document.getElementById('openPayModalBtn');
    const modalEl = document.getElementById('payInvoiceModal');
    const form = document.getElementById('payInvoiceForm');
    const outDisplay = document.getElementById('modalOutstandingDisplay');
    const amountInput = document.getElementById('paymentAmountInput');
    const methodSelect = document.getElementById('paymentMethodSelect');
    const txInput = document.getElementById('paymentTxRefInput');
    const execBtn = document.getElementById('executePayBtn');

    if (!modalEl || !openBtn) return;
    const bsModal = new window.bootstrap.Modal(modalEl);

    openBtn.onclick = () => {
        const balance = inv.outstandingBalance || 0;
        outDisplay.textContent = formatCurrency(balance);
        amountInput.value = balance.toFixed(2);
        amountInput.max = balance;
        txInput.value = 'TXN-' + Math.random().toString(36).substring(2, 10).toUpperCase();
        bsModal.show();
    };

    form.onsubmit = async (e) => {
        e.preventDefault();
        const amount = Number(amountInput.value);
        const method = methodSelect.value;
        const txRef = txInput.value.trim();

        if (amount <= 0 || amount > (inv.outstandingBalance || 0)) {
            toast.error('Payment amount must be greater than zero and cannot exceed outstanding balance.');
            return;
        }

        execBtn.disabled = true;
        execBtn.textContent = 'Processing...';

        try {
            await api.post(`/api/v1/invoices/${inv.id}/pay`, {
                amount,
                paymentMethod: method,
                transactionReference: txRef
            });

            toast.success('Payment successfully processed! Statement updated.');
            bsModal.hide();
            await loadInvoiceDetail(inv.id, false);
        } catch (err) {
            toast.error(err.message || 'Payment processing failed.');
        } finally {
            execBtn.disabled = false;
            execBtn.textContent = 'Confirm Payment';
        }
    };
}

