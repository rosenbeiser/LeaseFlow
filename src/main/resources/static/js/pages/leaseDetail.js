/**
 * Lease Detail & State Lifecycle Management Module
 */

import { api } from '../api.js';
import { auth } from '../auth.js';
import { renderLandlordLayout, renderTenantLayout, bindLayoutEvents } from '../layout.js';
import { renderLeaseBadge } from '../ui/badge.js';
import { renderLeaseStepper } from '../ui/stepper.js';
import { modal } from '../ui/modal.js';
import { escapeHtml, formatCurrency, formatDate } from '../ui/form.js';
import { toast } from '../ui/toast.js';

export async function renderLeaseDetail(params) {
    const leaseId = params.id;
    const isLandlord = auth.isLandlord() || auth.isAdmin();

    const renderLayout = isLandlord ? renderLandlordLayout : renderTenantLayout;

    const appEl = document.getElementById('app');
    appEl.innerHTML = renderLayout(`
        <div class="mb-4">
            <a href="${isLandlord ? '#/landlord/leases' : '#/tenant/dashboard'}" class="btn btn-sm btn-outline-secondary mb-3">
                <i class="bi bi-arrow-left me-1"></i>Back
            </a>
            <div id="leaseDetailContainer">
                <div class="card p-5 text-center">
                    <div class="spinner-border text-primary mx-auto mb-2" role="status"></div>
                    <div class="text-muted">Loading lease agreement details...</div>
                </div>
            </div>
        </div>

        <!-- Settle Modal -->
        <div class="modal fade" id="settleLeaseModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow-lg">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold">Settle Terminated Lease</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <form id="settleLeaseForm">
                        <div class="modal-body p-4">
                            <div class="alert alert-info py-2 px-3 small mb-3">
                                <i class="bi bi-info-circle me-1"></i>
                                Settlement formula: <code>Refund = Security Deposit - Unpaid Balance - Damages</code>
                            </div>

                            <div class="mb-3">
                                <label class="form-label fw-semibold">Security Deposit on File</label>
                                <input type="text" class="form-control" id="settleDepositDisplay" readonly>
                            </div>

                            <div class="mb-3">
                                <label class="form-label fw-semibold">Damages / Repair Deductions ($)</label>
                                <input type="number" step="0.01" min="0" class="form-control tabular-nums" id="settleDamagesInput" value="0.00" required>
                            </div>

                            <div class="mb-3">
                                <label class="form-label fw-semibold">Settlement Notes</label>
                                <textarea class="form-control" id="settleNotesInput" rows="2" placeholder="e.g. Clean handover, key returned, deductions for wall paint"></textarea>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="submit" class="btn btn-danger" id="executeSettleBtn">Execute Settlement</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>

        <!-- Generate Invoice Modal -->
        <div class="modal fade" id="generateInvoiceModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow-lg">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold">Generate Monthly Invoice</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <form id="generateInvoiceForm">
                        <div class="modal-body p-4">
                            <p class="text-muted small">
                                Calculates base rent plus metered utility charges for the selected billing month using the Strategy pattern.
                            </p>
                            <div class="mb-3">
                                <label class="form-label fw-semibold">Billing Month (YYYY-MM)</label>
                                <input type="month" class="form-control" id="invoiceMonthInput" required>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="submit" class="btn btn-primary" id="executeGenerateInvoiceBtn">Generate Invoice</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    `, isLandlord ? 'leases' : 'dashboard');
    bindLayoutEvents();

    await loadLeaseDetail(leaseId, isLandlord);
}

async function loadLeaseDetail(leaseId, isLandlord) {
    const container = document.getElementById('leaseDetailContainer');
    if (!container) return;

    try {
        const lease = await api.get(`/api/v1/leases/${leaseId}`);
        const allowedActions = lease.allowedActions || [];

        container.innerHTML = `
            <div class="row g-4">
                <!-- Main Agreement Details -->
                <div class="col-lg-8">
                    <div class="card mb-4">
                        <div class="card-header d-flex flex-wrap justify-content-between align-items-center gap-2">
                            <div class="d-flex align-items-center gap-2">
                                <h5 class="mb-0 fw-bold">Lease #LSE-${lease.id}</h5>
                                ${renderLeaseBadge(lease.status)}
                            </div>
                            <div class="text-muted small">Created ${formatDate(lease.createdAt)}</div>
                        </div>
                        <div class="card-body">
                            <div class="row g-3 mb-4">
                                <div class="col-sm-6">
                                    <div class="p-3 rounded bg-body-tertiary border">
                                        <div class="small text-muted mb-1"><i class="bi bi-building me-1"></i>Property &amp; Unit</div>
                                        <div class="fw-bold fs-6">${escapeHtml(lease.propertyName)}</div>
                                        <div class="text-secondary small">Unit ${escapeHtml(lease.unitNumber)}</div>
                                    </div>
                                </div>
                                <div class="col-sm-6">
                                    <div class="p-3 rounded bg-body-tertiary border">
                                        <div class="small text-muted mb-1"><i class="bi bi-person me-1"></i>Tenant Information</div>
                                        <div class="fw-bold fs-6">${escapeHtml(lease.tenantName)}</div>
                                        <div class="text-secondary small">${escapeHtml(lease.tenantEmail)}</div>
                                    </div>
                                </div>
                            </div>

                            <div class="row g-3 py-3 border-top border-bottom mb-4">
                                <div class="col-6 col-md-3">
                                    <div class="small text-muted">Start Date</div>
                                    <div class="fw-semibold">${formatDate(lease.startDate)}</div>
                                </div>
                                <div class="col-6 col-md-3">
                                    <div class="small text-muted">End Date</div>
                                    <div class="fw-semibold">${formatDate(lease.endDate)}</div>
                                </div>
                                <div class="col-6 col-md-3">
                                    <div class="small text-muted">Monthly Base Rent</div>
                                    <div class="fw-bold text-primary fs-5 tabular-nums">${formatCurrency(lease.baseRent)}</div>
                                </div>
                                <div class="col-6 col-md-3">
                                    <div class="small text-muted">Security Deposit</div>
                                    <div class="fw-bold fs-5 tabular-nums">${formatCurrency(lease.securityDeposit)}</div>
                                </div>
                            </div>

                            <!-- Invoices for this lease -->
                            <div class="d-flex justify-content-between align-items-center mb-3">
                                <h6 class="fw-bold mb-0">Billing &amp; Invoices</h6>
                                ${isLandlord && allowedActions.includes('GENERATE_INVOICE') ? `
                                    <button class="btn btn-sm btn-primary" id="openGenerateInvoiceBtn">
                                        <i class="bi bi-receipt me-1"></i>Generate Invoice
                                    </button>
                                ` : ''}
                            </div>
                            <div id="leaseInvoicesContainer">
                                <div class="small text-muted">Loading invoices...</div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Right Column: State Machine Actions & Lifecycle Timeline -->
                <div class="col-lg-4">
                    <!-- Actions Card (strictly from allowedActions) -->
                    ${isLandlord ? `
                        <div class="card mb-4">
                            <div class="card-header fw-semibold">
                                <i class="bi bi-lightning-charge me-1 text-warning"></i>Permitted Lifecycle Actions
                            </div>
                            <div class="card-body">
                                <p class="text-muted small mb-3">
                                    The state pattern strictly governs transitions. Only actions permitted in <strong>${escapeHtml(lease.status)}</strong> are available below:
                                </p>
                                <div class="d-grid gap-2" id="actionButtonsContainer">
                                    ${allowedActions.length === 0 ? `
                                        <div class="text-muted small p-2 bg-body-tertiary rounded text-center">
                                            No transitions permitted in ${escapeHtml(lease.status)} state.
                                        </div>
                                    ` : ''}

                                    ${allowedActions.includes('ACTIVATE') ? `
                                        <button class="btn btn-success fw-semibold" id="btnActivate">
                                            <i class="bi bi-check-circle me-1"></i>Activate Lease
                                        </button>
                                    ` : ''}

                                    ${allowedActions.includes('SERVE_NOTICE') ? `
                                        <button class="btn btn-warning fw-semibold" id="btnServeNotice">
                                            <i class="bi bi-bell me-1"></i>Serve Vacancy Notice
                                        </button>
                                    ` : ''}

                                    ${allowedActions.includes('TERMINATE') ? `
                                        <button class="btn btn-outline-danger fw-semibold" id="btnTerminate">
                                            <i class="bi bi-x-octagon me-1"></i>Terminate Lease
                                        </button>
                                    ` : ''}

                                    ${allowedActions.includes('SETTLE') ? `
                                        <button class="btn btn-danger fw-semibold" id="btnSettle">
                                            <i class="bi bi-cash-stack me-1"></i>Final Settlement
                                        </button>
                                    ` : ''}
                                </div>
                            </div>
                        </div>
                    ` : ''}

                    <!-- Vertical Timeline Stepper -->
                    <div class="card">
                        <div class="card-header fw-semibold">
                            <i class="bi bi-clock-history me-1 text-primary"></i>Lifecycle Audit Timeline
                        </div>
                        <div class="card-body">
                            ${renderLeaseStepper(lease.statusHistory, lease.status)}
                        </div>
                    </div>
                </div>
            </div>
        `;

        bindStateActionButtons(lease, isLandlord);
        await loadLeaseInvoices(lease.id);

    } catch (err) {
        container.innerHTML = `
            <div class="alert alert-danger">
                Failed to load lease details: ${escapeHtml(err.message)}
            </div>
        `;
    }
}

async function loadLeaseInvoices(leaseId) {
    const container = document.getElementById('leaseInvoicesContainer');
    if (!container) return;

    try {
        const invoices = await api.get(`/api/v1/invoices/lease/${leaseId}`);
        if (!invoices || invoices.length === 0) {
            container.innerHTML = '<div class="text-muted small py-2">No invoices generated for this lease agreement yet.</div>';
            return;
        }

        container.innerHTML = `
            <div class="table-responsive">
                <table class="table table-sm table-hover align-middle mb-0">
                    <thead class="table-light small">
                        <tr>
                            <th>Invoice #</th>
                            <th>Month</th>
                            <th>Total</th>
                            <th>Paid</th>
                            <th>Status</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        ${invoices.map(inv => `
                            <tr>
                                <td class="font-monospace">#INV-${inv.id}</td>
                                <td>${escapeHtml(inv.billingMonth)}</td>
                                <td class="fw-semibold tabular-nums">${formatCurrency(inv.totalPayable)}</td>
                                <td class="tabular-nums text-muted">${formatCurrency(inv.amountPaid)}</td>
                                <td>${renderLeaseBadge(inv.status)}</td>
                                <td class="text-end">
                                    <a href="#/invoices/${inv.id}" class="btn btn-xs btn-outline-secondary py-0 px-2" style="font-size: 0.75rem;">
                                        View
                                    </a>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch {
        container.innerHTML = '<div class="text-muted small">Invoices unavailable.</div>';
    }
}

function bindStateActionButtons(lease, isLandlord) {
    if (!isLandlord) return;

    // 1. ACTIVATE
    const actBtn = document.getElementById('btnActivate');
    if (actBtn) {
        actBtn.onclick = async () => {
            const confirmed = await modal.confirm({
                title: 'Activate Lease Agreement',
                message: `Are you sure you want to activate Lease #LSE-${lease.id}? This will change the unit occupancy status to OCCUPIED and commence monthly billing.`,
                confirmText: 'Activate Lease'
            });
            if (confirmed) {
                try {
                    await api.put(`/api/v1/leases/${lease.id}/activate`);
                    toast.success('Lease successfully activated!');
                    await loadLeaseDetail(lease.id, true);
                } catch (err) {
                    toast.error(err.message || 'Activation failed.');
                }
            }
        };
    }

    // 2. SERVE NOTICE
    const noticeBtn = document.getElementById('btnServeNotice');
    if (noticeBtn) {
        noticeBtn.onclick = async () => {
            const confirmed = await modal.confirm({
                title: 'Serve Vacancy Notice',
                message: `Serve formal vacancy notice on Lease #LSE-${lease.id}? The lease status will move to NOTICE_SERVED. Invoicing is still permitted until termination.`,
                confirmText: 'Serve Notice',
                isDanger: false
            });
            if (confirmed) {
                try {
                    await api.put(`/api/v1/leases/${lease.id}/notice`);
                    toast.success('Notice served successfully.');
                    await loadLeaseDetail(lease.id, true);
                } catch (err) {
                    toast.error(err.message || 'Failed to serve notice.');
                }
            }
        };
    }

    // 3. TERMINATE
    const termBtn = document.getElementById('btnTerminate');
    if (termBtn) {
        termBtn.onclick = async () => {
            const confirmed = await modal.confirm({
                title: 'Terminate Lease Agreement',
                message: `Terminate Lease #LSE-${lease.id}? The unit will become VACANT. No further invoices can be issued after termination. You can then proceed with deposit settlement.`,
                confirmText: 'Terminate Agreement',
                isDanger: true
            });
            if (confirmed) {
                try {
                    await api.put(`/api/v1/leases/${lease.id}/terminate`);
                    toast.success('Lease terminated. Unit marked as vacant.');
                    await loadLeaseDetail(lease.id, true);
                } catch (err) {
                    toast.error(err.message || 'Failed to terminate lease.');
                }
            }
        };
    }

    // 4. SETTLE
    const settleBtn = document.getElementById('btnSettle');
    const settleModalEl = document.getElementById('settleLeaseModal');
    const settleForm = document.getElementById('settleLeaseForm');
    const depositDisplay = document.getElementById('settleDepositDisplay');
    const damagesInput = document.getElementById('settleDamagesInput');
    const notesInput = document.getElementById('settleNotesInput');

    if (settleBtn && settleModalEl) {
        const bsModal = new window.bootstrap.Modal(settleModalEl);

        settleBtn.onclick = () => {
            depositDisplay.value = formatCurrency(lease.securityDeposit);
            damagesInput.value = '0.00';
            notesInput.value = '';
            bsModal.show();
        };

        settleForm.onsubmit = async (e) => {
            e.preventDefault();
            const damagesAmount = Number(damagesInput.value) || 0;
            const notes = notesInput.value.trim();

            const execBtn = document.getElementById('executeSettleBtn');
            execBtn.disabled = true;
            execBtn.textContent = 'Settling...';

            try {
                const res = await api.post(`/api/v1/leases/${lease.id}/settle`, {
                    damagesAmount,
                    notes
                });

                bsModal.hide();
                toast.success(res.tenantOwes
                    ? `Settlement complete. Tenant balance due: ${formatCurrency(res.refundAmount)}.`
                    : `Settlement complete. Tenant refund calculated: ${formatCurrency(res.refundAmount)}.`);
                await loadLeaseDetail(lease.id, true);
            } catch (err) {
                toast.error(err.message || 'Settlement failed.');
            } finally {
                execBtn.disabled = false;
                execBtn.textContent = 'Execute Settlement';
            }
        };
    }

    // 5. GENERATE INVOICE MODAL
    const openInvBtn = document.getElementById('openGenerateInvoiceBtn');
    const invModalEl = document.getElementById('generateInvoiceModal');
    const invForm = document.getElementById('generateInvoiceForm');
    const monthInput = document.getElementById('invoiceMonthInput');

    if (openInvBtn && invModalEl) {
        const bsModal = new window.bootstrap.Modal(invModalEl);

        openInvBtn.onclick = () => {
            const now = new Date();
            const ym = now.toISOString().slice(0, 7);
            monthInput.value = ym;
            bsModal.show();
        };

        invForm.onsubmit = async (e) => {
            e.preventDefault();
            const month = monthInput.value;
            if (!month) return;

            const execBtn = document.getElementById('executeGenerateInvoiceBtn');
            execBtn.disabled = true;
            execBtn.textContent = 'Generating...';

            try {
                const invoice = await api.post(`/api/v1/invoices/generate/${lease.id}?month=${month}`);
                toast.success(`Invoice generated for month ${month}!`);
                bsModal.hide();
                window.location.hash = `#/invoices/${invoice.id}`;
            } catch (err) {
                toast.error(err.message || 'Failed to generate invoice.');
            } finally {
                execBtn.disabled = false;
                execBtn.textContent = 'Generate Invoice';
            }
        };
    }
}

