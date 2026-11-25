/**
 * Landlord Dashboard Page Module
 */

import { api } from '../api.js';
import { renderLandlordLayout, bindLayoutEvents } from '../layout.js';
import { renderKpiCard } from '../ui/kpi.js';
import { renderSkeletonCards, renderSkeletonTable } from '../ui/skeleton.js';
import { renderInvoiceBadge } from '../ui/badge.js';
import { escapeHtml, formatCurrency, formatDate } from '../ui/form.js';
import { toast } from '../ui/toast.js';

let revenueChartInstance = null;
let occupancyChartInstance = null;

export async function renderLandlordDashboard() {
    const appEl = document.getElementById('app');
    appEl.innerHTML = renderLandlordLayout(`
        <div id="dashboardContainer">
            ${renderSkeletonCards(4)}
            ${renderSkeletonTable(5, 5)}
        </div>
    `, 'dashboard');
    bindLayoutEvents();

    try {
        const data = await api.get('/api/v1/dashboard/landlord');
        renderDashboardContent(data);
    } catch (err) {
        toast.error('Failed to load dashboard data: ' + err.message);
    }

    return () => {
        if (revenueChartInstance) {
            revenueChartInstance.destroy();
            revenueChartInstance = null;
        }
        if (occupancyChartInstance) {
            occupancyChartInstance.destroy();
            occupancyChartInstance = null;
        }
    };
}

function renderDashboardContent(data) {
    const container = document.getElementById('dashboardContainer');
    if (!container) return;

    const totalUnits = data.totalUnits || 0;
    const occupiedUnits = data.occupiedUnits || 0;
    const vacantUnits = data.vacantUnits || 0;
    const occupancyPct = data.occupancyPercentage || 0;
    const activeLeases = data.activeLeaseCount || 0;
    const outstanding = data.outstandingBalance || 0;
    const overdueCount = data.overdueInvoiceCount || 0;

    const recentInvoices = data.recentInvoices || [];
    const revenueByMonth = data.revenueByMonth || [];

    container.innerHTML = `
        <!-- KPI Row -->
        <div class="row g-3 mb-4">
            <div class="col-sm-6 col-xl-3">
                ${renderKpiCard({
                    title: 'Occupancy Rate',
                    value: `${occupancyPct}%`,
                    icon: 'bi-pie-chart',
                    subtitle: `${occupiedUnits} occupied / ${totalUnits} units`
                })}
            </div>
            <div class="col-sm-6 col-xl-3">
                ${renderKpiCard({
                    title: 'Active Leases',
                    value: activeLeases,
                    icon: 'bi-file-earmark-check',
                    subtitle: 'Units under agreement'
                })}
            </div>
            <div class="col-sm-6 col-xl-3">
                ${renderKpiCard({
                    title: 'Outstanding Balance',
                    value: outstanding,
                    isCurrency: true,
                    icon: 'bi-cash-coin',
                    subtitle: 'Pending & overdue payments'
                })}
            </div>
            <div class="col-sm-6 col-xl-3">
                ${renderKpiCard({
                    title: 'Overdue Invoices',
                    value: overdueCount,
                    icon: 'bi-exclamation-triangle',
                    subtitle: overdueCount > 0 ? 'Action required' : 'All accounts current'
                })}
            </div>
        </div>

        <!-- Charts Row -->
        <div class="row g-4 mb-4">
            <div class="col-lg-8">
                <div class="card h-100">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <span class="fw-semibold">Revenue Collected (Last 6 Months)</span>
                        <span class="badge bg-primary-subtle text-primary border border-primary-subtle">USD</span>
                    </div>
                    <div class="card-body">
                        <div style="height: 280px; position: relative;">
                            <canvas id="revenueChart"></canvas>
                        </div>
                    </div>
                </div>
            </div>

            <div class="col-lg-4">
                <div class="card h-100">
                    <div class="card-header fw-semibold">Portfolio Occupancy</div>
                    <div class="card-body d-flex flex-column align-items-center justify-content-center">
                        <div style="height: 220px; width: 220px; position: relative;">
                            <canvas id="occupancyChart"></canvas>
                        </div>
                        <div class="d-flex justify-content-center gap-4 mt-3 small">
                            <div><i class="bi bi-circle-fill text-primary me-1"></i>Occupied (${occupiedUnits})</div>
                            <div><i class="bi bi-circle-fill text-teal me-1" style="color: #0d9488;"></i>Vacant (${vacantUnits})</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Recent Invoices Table -->
        <div class="card">
            <div class="card-header d-flex justify-content-between align-items-center">
                <span class="fw-semibold">Recent Invoices</span>
                <a href="#/landlord/invoices" class="btn btn-sm btn-outline-primary">View All Invoices</a>
            </div>
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th>Invoice #</th>
                            <th>Month</th>
                            <th>Property &amp; Unit</th>
                            <th>Tenant</th>
                            <th>Total</th>
                            <th>Status</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${recentInvoices.length === 0 ? `
                            <tr><td colspan="7" class="text-center py-4 text-muted">No recent invoices recorded.</td></tr>
                        ` : recentInvoices.map(inv => `
                            <tr>
                                <td class="fw-medium font-monospace">#INV-${inv.id}</td>
                                <td>${escapeHtml(inv.billingMonth || '—')}</td>
                                <td>${escapeHtml(inv.propertyName || '—')} <span class="badge bg-light text-dark border ms-1">${escapeHtml(inv.unitNumber || '—')}</span></td>
                                <td>${escapeHtml(inv.tenantName || '—')}</td>
                                <td class="fw-semibold tabular-nums">${formatCurrency(inv.totalPayable)}</td>
                                <td>${renderInvoiceBadge(inv.status)}</td>
                                <td>
                                    <a href="#/invoices/${inv.id}" class="btn btn-sm btn-outline-secondary">
                                        <i class="bi bi-eye"></i>
                                    </a>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;

    initCharts(revenueByMonth, occupiedUnits, vacantUnits);
}

function initCharts(revenueByMonth, occupied, vacant) {
    // 1. Revenue Bar Chart
    const revCanvas = document.getElementById('revenueChart');
    if (revCanvas && window.Chart) {
        if (revenueChartInstance) revenueChartInstance.destroy();

        const labels = revenueByMonth.map(r => r.month || '');
        const dataVals = revenueByMonth.map(r => r.collected || 0);

        revenueChartInstance = new window.Chart(revCanvas, {
            type: 'bar',
            data: {
                labels: labels.length > 0 ? labels : ['Past Months'],
                datasets: [{
                    label: 'Revenue ($)',
                    data: dataVals.length > 0 ? dataVals : [0],
                    backgroundColor: '#4f46e5',
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: (v) => '$' + v.toLocaleString()
                        }
                    }
                }
            }
        });
    }

    // 2. Occupancy Doughnut Chart
    const occCanvas = document.getElementById('occupancyChart');
    if (occCanvas && window.Chart) {
        if (occupancyChartInstance) occupancyChartInstance.destroy();

        const hasUnits = (occupied + vacant) > 0;
        occupancyChartInstance = new window.Chart(occCanvas, {
            type: 'doughnut',
            data: {
                labels: ['Occupied', 'Vacant'],
                datasets: [{
                    data: hasUnits ? [occupied, vacant] : [0, 1],
                    backgroundColor: ['#4f46e5', '#0d9488'],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '72%',
                plugins: {
                    legend: { display: false }
                }
            }
        });
    }
}

