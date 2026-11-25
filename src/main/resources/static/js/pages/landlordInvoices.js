/**
 * Landlord Invoices Listing & Generation Module
 */

import { api } from '../api.js';
import { renderLandlordLayout, bindLayoutEvents } from '../layout.js';
import { renderSkeletonTable } from '../ui/skeleton.js';
import { renderEmptyState } from '../ui/empty.js';
import { renderInvoiceBadge } from '../ui/badge.js';
import { escapeHtml, formatCurrency, formatDate } from '../ui/form.js';
import { toast } from '../ui/toast.js';

let currentStatusFilter = '';
let currentMonthFilter = '';

export async function renderLandlordInvoices() {
    const appEl = document.getElementById('app');
    appEl.innerHTML = renderLandlordLayout(`
        <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">Invoices &amp; Billing</h4>
                <p class="text-muted mb-0 small">Monthly bill statements, payments, and overdue tracking</p>
            </div>
            <button class="btn btn-primary" id="openGenerateInvoiceModalBtn">
                <i class="bi bi-receipt me-1"></i>Generate Invoice
            </button>
        </div>

        <!-- Filter Bar -->
        <div class="card p-3 mb-4">
            <div class="row g-2 align-items-center">
                <div class="col-md-5">
                    <label class="form-label small text-muted mb-1">Filter Status</label>
                    <select class="form-select form-select-sm" id="invoiceStatusFilter">
                        <option value="">All Statuses</option>
                        <option value="PENDING">Pending</option>
                        <option value="PARTIALLY_PAID">Partially Paid</option>
                        <option value="PAID">Paid</option>
                        <option value="OVERDUE">Overdue</option>
                    </select>
                </div>
                <div class="col-md-5">
                    <label class="form-label small text-muted mb-1">Filter Month</label>
                    <input type="month" class="form-control form-control-sm" id="invoiceMonthFilter">
                </div>
                <div class="col-md-2 d-flex align-items-end">
                    <button class="btn btn-sm btn-outline-secondary w-100" id="resetInvoiceFiltersBtn">Reset</button>
                </div>
            </div>
        </div>

        <div id="invoicesTableContainer">
            ${renderSkeletonTable(6, 7)}
        </div>

        <!-- Quick Generate Invoice Modal -->
        <div class="modal fade" id="quickGenerateModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow-lg">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold">Generate Monthly Invoice</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <form id="quickGenerateForm">
                        <div class="modal-body p-4">
                            <div class="mb-3">
                                <label class="form-label fw-semibold">Active Lease Agreement</label>
                                <select class="form-select" id="quickLeaseSelect" required></select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label fw-semibold">Billing Month (YYYY-MM)</label>
                                <input type="month" class="form-control" id="quickMonthInput" required>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="submit" class="btn btn-primary" id="executeQuickGenerateBtn">Generate Statement</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    `, 'invoices');
    bindLayoutEvents();

    initInvoiceFilters();
    initQuickGenerateModal();
    await loadInvoices();
}

function initInvoiceFilters() {
    const statusSelect = document.getElementById('invoiceStatusFilter');
    const monthInput = document.getElementById('invoiceMonthFilter');
    const resetBtn = document.getElementById('resetInvoiceFiltersBtn');

    if (statusSelect) {
        statusSelect.onchange = () => {
            currentStatusFilter = statusSelect.value;
            loadInvoices();
        };
    }
    if (monthInput) {
        monthInput.onchange = () => {
            currentMonthFilter = monthInput.value;
            loadInvoices();
        };
    }
    if (resetBtn) {
        resetBtn.onclick = () => {
            currentStatusFilter = '';
            currentMonthFilter = '';
            if (statusSelect) statusSelect.value = '';
            if (monthInput) monthInput.value = '';
            loadInvoices();
        };
    }
}

async function loadInvoices() {
    const container = document.getElementById('invoicesTableContainer');
    if (!container) return;

    try {
        const params = { size: 50 };
        if (currentStatusFilter) params.status = currentStatusFilter;
        if (currentMonthFilter) params.month = currentMonthFilter;

        const res = await api.get('/api/v1/invoices', params);
        const invoices = res.content || [];

        if (invoices.length === 0) {
            container.innerHTML = renderEmptyState({
                title: 'No Invoices Found',
                description: 'No billing statements match the selected criteria.',
                actionText: 'Generate Invoice',
                actionBtnId: 'emptyGenInvoiceBtn'
            });
            const emptyBtn = document.getElementById('emptyGenInvoiceBtn');
            if (emptyBtn) {
                emptyBtn.onclick = () => {
                    const modalEl = new window.bootstrap.Modal(document.getElementById('quickGenerateModal'));
                    modalEl.show();
                };
            }
            return;
        }

        container.innerHTML = `
            <div class="card p-3">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th>Invoice #</th>
                                <th>Month</th>
                                <th>Property &amp; Unit</th>
                                <th>Tenant</th>
                                <th>Due Date</th>
                                <th>Total</th>
                                <th>Paid</th>
                                <th>Balance</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${invoices.map(inv => `
                                <tr>
                                    <td class="font-monospace fw-semibold">#INV-${inv.id}</td>
                                    <td><span class="badge bg-light text-dark border font-monospace">${escapeHtml(inv.billingMonth)}</span></td>
                                    <td>${escapeHtml(inv.propertyName)} <span class="text-muted small">(${escapeHtml(inv.unitNumber)})</span></td>
                                    <td>${escapeHtml(inv.tenantName)}</td>
                                    <td class="small">${formatDate(inv.dueDate)}</td>
                                    <td class="fw-bold tabular-nums">${formatCurrency(inv.totalPayable)}</td>
                                    <td class="tabular-nums text-muted">${formatCurrency(inv.amountPaid)}</td>
                                    <td class="fw-semibold tabular-nums text-danger">${formatCurrency(inv.outstandingBalance)}</td>
                                    <td>${renderInvoiceBadge(inv.status)}</td>
                                    <td>
                                        <a href="#/invoices/${inv.id}" class="btn btn-sm btn-outline-primary">
                                            <i class="bi bi-file-earmark-text me-1"></i>View
                                        </a>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    } catch (err) {
        toast.error('Failed to load invoices: ' + err.message);
    }
}

function initQuickGenerateModal() {
    const openBtn = document.getElementById('openGenerateInvoiceModalBtn');
    const modalEl = document.getElementById('quickGenerateModal');
    const form = document.getElementById('quickGenerateForm');
    const leaseSelect = document.getElementById('quickLeaseSelect');
    const monthInput = document.getElementById('quickMonthInput');
    const execBtn = document.getElementById('executeQuickGenerateBtn');

    if (!modalEl) return;
    const bsModal = new window.bootstrap.Modal(modalEl);

    if (openBtn) {
        openBtn.onclick = async () => {
            form.reset();
            monthInput.value = new Date().toISOString().slice(0, 7);
            bsModal.show();

            try {
                const res = await api.get('/api/v1/leases', { status: 'ACTIVE', size: 50 });
                const leases = res.content || [];
                if (leases.length === 0) {
                    leaseSelect.innerHTML = '<option value="">No ACTIVE leases available</option>';
                    return;
                }
                leaseSelect.innerHTML = leases.map(l => `
                    <option value="${l.id}">Lease #${l.id} - ${escapeHtml(l.propertyName)} (Unit ${escapeHtml(l.unitNumber)}) - ${escapeHtml(l.tenantName)}</option>
                `).join('');
            } catch {
                leaseSelect.innerHTML = '<option value="">Failed to load leases</option>';
            }
        };
    }

    if (form) {
        form.onsubmit = async (e) => {
            e.preventDefault();
            const leaseId = leaseSelect.value;
            const month = monthInput.value;

            if (!leaseId || !month) {
                toast.warning('Please select a lease agreement and billing month.');
                return;
            }

            execBtn.disabled = true;
            execBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Generating...';

            try {
                const invoice = await api.post(`/api/v1/invoices/generate/${leaseId}?month=${month}`);
                toast.success(`Invoice generated successfully!`);
                bsModal.hide();
                window.location.hash = `#/invoices/${invoice.id}`;
            } catch (err) {
                toast.error(err.message || 'Invoice generation failed.');
            } finally {
                execBtn.disabled = false;
                execBtn.innerHTML = 'Generate Statement';
            }
        };
    }
}

