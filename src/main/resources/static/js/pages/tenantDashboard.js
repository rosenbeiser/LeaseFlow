/**
 * Tenant Dashboard Page Module
 */

import { api } from '../api.js';
import { renderTenantLayout, bindLayoutEvents } from '../layout.js';
import { renderKpiCard } from '../ui/kpi.js';
import { renderSkeletonCards } from '../ui/skeleton.js';
import { renderLeaseBadge, renderInvoiceBadge } from '../ui/badge.js';
import { escapeHtml, formatCurrency, formatDate, formatDateTime } from '../ui/form.js';
import { toast } from '../ui/toast.js';

export async function renderTenantDashboard() {
    const appEl = document.getElementById('app');
    appEl.innerHTML = renderTenantLayout(`
        <div id="tenantDashboardContainer">
            ${renderSkeletonCards(3)}
        </div>
    `, 'dashboard');
    bindLayoutEvents();

    try {
        const data = await api.get('/api/v1/dashboard/tenant');
        renderContent(data);
    } catch (err) {
        toast.error('Failed to load dashboard: ' + err.message);
    }
}

function renderContent(data) {
    const container = document.getElementById('tenantDashboardContainer');
    if (!container) return;

    const lease = data.currentLease;
    const nextDue = data.nextDueInvoice;
    const outstanding = data.outstandingBalance || 0;
    const recentPayments = data.recentPayments || [];

    // Calculate due warning/countdown
    let dueBadgeHtml = '';
    if (nextDue) {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const dueDate = new Date(nextDue.dueDate);
        const diffTime = dueDate - today;
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

        if (diffDays < 0) {
            dueBadgeHtml = `<span class="badge bg-danger"><i class="bi bi-exclamation-triangle me-1"></i>Overdue by ${Math.abs(diffDays)} days</span>`;
        } else if (diffDays === 0) {
            dueBadgeHtml = `<span class="badge bg-warning text-dark"><i class="bi bi-clock me-1"></i>Due Today</span>`;
        } else {
            dueBadgeHtml = `<span class="badge bg-info text-dark"><i class="bi bi-calendar-event me-1"></i>Due in ${diffDays} days</span>`;
        }
    }

    container.innerHTML = `
        <!-- Welcome Banner -->
        <div class="card bg-primary text-white border-0 p-4 mb-4" style="background: linear-gradient(135deg, #4f46e5 0%, #312e81 100%);">
            <div class="row align-items-center">
                <div class="col-md-8">
                    <span class="badge bg-white bg-opacity-25 mb-2">Tenant Portal</span>
                    <h3 class="fw-bold mb-1">Welcome to Your Resident Hub</h3>
                    <p class="mb-0 opacity-75 small">
                        Track your rental agreement, review itemized utility statements, and submit instant balance payments.
                    </p>
                </div>
                <div class="col-md-4 text-md-end mt-3 mt-md-0">
                    <a href="#/tenant/bills" class="btn btn-light fw-semibold text-primary">
                        <i class="bi bi-receipt me-1"></i>View Bills &amp; Pay
                    </a>
                </div>
            </div>
        </div>

        <!-- Metric KPI Cards -->
        <div class="row g-3 mb-4">
            <div class="col-md-4">
                ${renderKpiCard({
                    title: 'Total Outstanding Balance',
                    value: outstanding,
                    isCurrency: true,
                    icon: 'bi-wallet2',
                    subtitle: outstanding > 0 ? 'Payment pending' : 'Zero balance — All caught up!'
                })}
            </div>
            <div class="col-md-4">
                <div class="card kpi-card h-100">
                    <div class="kpi-title">Next Bill Due</div>
                    ${nextDue ? `
                        <div class="kpi-value text-danger tabular-nums">${formatCurrency(nextDue.outstandingBalance)}</div>
                        <div class="d-flex align-items-center gap-2 mt-2">
                            ${dueBadgeHtml}
                            <span class="small text-muted">${formatDate(nextDue.dueDate)}</span>
                        </div>
                    ` : `
                        <div class="kpi-value text-success fs-4">No Bills Due</div>
                        <div class="small text-muted mt-2">You are fully paid up for this period.</div>
                    `}
                    <div class="kpi-icon"><i class="bi bi-calendar2-check"></i></div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card kpi-card h-100">
                    <div class="kpi-title">Current Lease Status</div>
                    ${lease ? `
                        <div class="d-flex align-items-center gap-2 mt-1">
                            ${renderLeaseBadge(lease.status)}
                        </div>
                        <div class="small text-muted mt-2">
                            ${escapeHtml(lease.propertyName)} (Unit ${escapeHtml(lease.unitNumber)})
                        </div>
                    ` : `
                        <div class="text-muted small mt-2">No active lease agreement found.</div>
                    `}
                    <div class="kpi-icon"><i class="bi bi-house-door"></i></div>
                </div>
            </div>
        </div>

        <div class="row g-4">
            <!-- Left: Current Lease Agreement Card -->
            <div class="col-lg-7">
                <div class="card h-100">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <span class="fw-semibold"><i class="bi bi-file-earmark-text me-1 text-primary"></i>My Lease Agreement</span>
                        ${lease ? `<a href="#/leases/${lease.id}" class="btn btn-sm btn-outline-primary">View Agreement Details</a>` : ''}
                    </div>
                    <div class="card-body">
                        ${lease ? `
                            <div class="row g-3 mb-3">
                                <div class="col-sm-6">
                                    <div class="p-3 rounded bg-body-tertiary border">
                                        <div class="small text-muted">Rental Property</div>
                                        <div class="fw-bold fs-6">${escapeHtml(lease.propertyName)}</div>
                                        <div class="small text-secondary">Unit ${escapeHtml(lease.unitNumber)}</div>
                                    </div>
                                </div>
                                <div class="col-sm-6">
                                    <div class="p-3 rounded bg-body-tertiary border">
                                        <div class="small text-muted">Monthly Rent &amp; Deposit</div>
                                        <div class="fw-bold fs-6 tabular-nums text-primary">${formatCurrency(lease.baseRent)} / mo</div>
                                        <div class="small text-secondary tabular-nums">Deposit: ${formatCurrency(lease.securityDeposit)}</div>
                                    </div>
                                </div>
                            </div>

                            <div class="d-flex justify-content-between py-2 border-top border-bottom small">
                                <div><span class="text-muted">Start Date:</span> <strong>${formatDate(lease.startDate)}</strong></div>
                                <div><span class="text-muted">End Date:</span> <strong>${formatDate(lease.endDate)}</strong></div>
                                <div><span class="text-muted">Agreement:</span> <strong>#LSE-${lease.id}</strong></div>
                            </div>
                        ` : `
                            <div class="p-4 text-center text-muted">
                                You currently have no active rental agreement attached to your account.
                            </div>
                        `}
                    </div>
                </div>
            </div>

            <!-- Right: Recent Payment Receipts -->
            <div class="col-lg-5">
                <div class="card h-100">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <span class="fw-semibold"><i class="bi bi-clock-history me-1 text-success"></i>Recent Payments</span>
                        <a href="#/tenant/payments" class="btn btn-sm btn-link text-decoration-none">All History</a>
                    </div>
                    <div class="card-body p-0">
                        ${recentPayments.length === 0 ? `
                            <div class="p-4 text-center text-muted small">No payment transactions recorded yet.</div>
                        ` : `
                            <div class="list-group list-group-flush">
                                ${recentPayments.map(p => `
                                    <div class="list-group-item d-flex justify-content-between align-items-center py-3">
                                        <div>
                                            <div class="fw-semibold text-success tabular-nums">+${formatCurrency(p.amountPaid)}</div>
                                            <div class="small text-muted font-monospace">${escapeHtml(p.transactionReference)}</div>
                                        </div>
                                        <div class="text-end">
                                            <span class="badge bg-light text-dark border">${escapeHtml(p.paymentMethod)}</span>
                                            <div class="small text-muted" style="font-size: 0.75rem;">${formatDate(p.paidAt)}</div>
                                        </div>
                                    </div>
                                `).join('')}
                            </div>
                        `}
                    </div>
                </div>
            </div>
        </div>
    `;
}

