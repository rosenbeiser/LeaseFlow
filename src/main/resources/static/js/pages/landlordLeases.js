/**
 * Landlord Leases Management Module
 */

import { api } from '../api.js';
import { renderLandlordLayout, bindLayoutEvents } from '../layout.js';
import { renderSkeletonTable } from '../ui/skeleton.js';
import { renderEmptyState } from '../ui/empty.js';
import { renderLeaseBadge } from '../ui/badge.js';
import { escapeHtml, formatCurrency, formatDate, setFormErrors, clearFormErrors } from '../ui/form.js';
import { toast } from '../ui/toast.js';

let currentFilterStatus = '';

export async function renderLandlordLeases() {
    const appEl = document.getElementById('app');
    appEl.innerHTML = renderLandlordLayout(`
        <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">Lease Agreements</h4>
                <p class="text-muted mb-0 small">Lifecycle management, state transitions, and tenant agreements</p>
            </div>
            <button class="btn btn-primary" id="openNewLeaseBtn">
                <i class="bi bi-plus-lg me-1"></i>New Lease Agreement
            </button>
        </div>

        <!-- Filter Bar -->
        <div class="card p-2 mb-4">
            <div class="d-flex flex-wrap gap-2 align-items-center">
                <span class="small fw-semibold text-muted px-2">Filter Status:</span>
                <button class="btn btn-sm ${currentFilterStatus === '' ? 'btn-primary' : 'btn-outline-secondary'} status-filter-btn" data-status="">All</button>
                <button class="btn btn-sm ${currentFilterStatus === 'DRAFT' ? 'btn-primary' : 'btn-outline-secondary'} status-filter-btn" data-status="DRAFT">Draft</button>
                <button class="btn btn-sm ${currentFilterStatus === 'ACTIVE' ? 'btn-primary' : 'btn-outline-secondary'} status-filter-btn" data-status="ACTIVE">Active</button>
                <button class="btn btn-sm ${currentFilterStatus === 'NOTICE_SERVED' ? 'btn-primary' : 'btn-outline-secondary'} status-filter-btn" data-status="NOTICE_SERVED">Notice Served</button>
                <button class="btn btn-sm ${currentFilterStatus === 'TERMINATED' ? 'btn-primary' : 'btn-outline-secondary'} status-filter-btn" data-status="TERMINATED">Terminated</button>
                <button class="btn btn-sm ${currentFilterStatus === 'SETTLED' ? 'btn-primary' : 'btn-outline-secondary'} status-filter-btn" data-status="SETTLED">Settled</button>
            </div>
        </div>

        <div id="leasesTableContainer">
            ${renderSkeletonTable(6, 6)}
        </div>

        <!-- Create Lease Wizard Modal -->
        <div class="modal fade" id="createLeaseModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-lg modal-dialog-centered">
                <div class="modal-content border-0 shadow-lg">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold">Create New Lease Agreement</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <form id="createLeaseForm" novalidate>
                        <div class="modal-body p-4">
                            <!-- 1. Unit Selection -->
                            <div class="mb-3">
                                <label class="form-label fw-semibold">Select Rental Unit</label>
                                <select class="form-select" name="unitId" id="leaseUnitSelect" required>
                                    <option value="">Loading available units...</option>
                                </select>
                                <div class="form-text">Only vacant units or units available for new leases can be selected.</div>
                            </div>

                            <!-- 2. Tenant Search -->
                            <div class="mb-3">
                                <label class="form-label fw-semibold">Assign Tenant by Email</label>
                                <div class="input-group">
                                    <input type="email" class="form-control" id="tenantSearchEmail" placeholder="tenant1@leaseflow.dev">
                                    <button class="btn btn-outline-secondary" type="button" id="searchTenantBtn">
                                        <i class="bi bi-search me-1"></i>Lookup
                                    </button>
                                </div>
                                <div id="tenantLookupResult" class="mt-2 small"></div>
                                <input type="hidden" name="tenantId" id="selectedTenantId" required>
                            </div>

                            <!-- 3. Dates & Rent -->
                            <div class="row g-3">
                                <div class="col-md-6">
                                    <label class="form-label fw-semibold">Start Date</label>
                                    <input type="date" class="form-control" name="startDate" required>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label fw-semibold">End Date</label>
                                    <input type="date" class="form-control" name="endDate" required>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label fw-semibold">Base Monthly Rent ($)</label>
                                    <input type="number" step="0.01" min="1" class="form-control tabular-nums" name="baseRent" required placeholder="1200.00">
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label fw-semibold">Security Deposit ($)</label>
                                    <input type="number" step="0.01" min="0" class="form-control tabular-nums" name="securityDeposit" required placeholder="2400.00">
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="submit" class="btn btn-primary" id="saveLeaseBtn">Create Lease (Draft)</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    `, 'leases');
    bindLayoutEvents();

    initFilterButtons();
    initCreateLeaseModal();
    await loadLeases();
}

function initFilterButtons() {
    document.querySelectorAll('.status-filter-btn').forEach(btn => {
        btn.onclick = async () => {
            currentFilterStatus = btn.getAttribute('data-status');
            document.querySelectorAll('.status-filter-btn').forEach(b => {
                b.className = 'btn btn-sm btn-outline-secondary status-filter-btn';
            });
            btn.className = 'btn btn-sm btn-primary status-filter-btn';
            await loadLeases();
        };
    });
}

async function loadLeases() {
    const container = document.getElementById('leasesTableContainer');
    if (!container) return;

    try {
        const params = { size: 50 };
        if (currentFilterStatus) params.status = currentFilterStatus;

        const res = await api.get('/api/v1/leases', params);
        const leases = res.content || [];

        if (leases.length === 0) {
            container.innerHTML = renderEmptyState({
                title: 'No Leases Found',
                description: currentFilterStatus ? `No leases with status ${currentFilterStatus}.` : 'Create your first lease agreement to start tracking tenant contracts.',
                actionText: 'New Lease Agreement',
                actionBtnId: 'emptyNewLeaseBtn'
            });
            const emptyBtn = document.getElementById('emptyNewLeaseBtn');
            if (emptyBtn) {
                emptyBtn.onclick = () => {
                    const modalEl = new window.bootstrap.Modal(document.getElementById('createLeaseModal'));
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
                                <th>Lease #</th>
                                <th>Property &amp; Unit</th>
                                <th>Tenant</th>
                                <th>Period</th>
                                <th>Monthly Rent</th>
                                <th>Status</th>
                                <th>Allowed Actions</th>
                                <th>Detail</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${leases.map(l => `
                                <tr>
                                    <td class="fw-medium font-monospace">#LSE-${l.id}</td>
                                    <td>
                                        <div class="fw-semibold">${escapeHtml(l.propertyName)}</div>
                                        <small class="text-muted">Unit ${escapeHtml(l.unitNumber)}</small>
                                    </td>
                                    <td>
                                        <div>${escapeHtml(l.tenantName)}</div>
                                        <small class="text-muted">${escapeHtml(l.tenantEmail)}</small>
                                    </td>
                                    <td class="small">
                                        ${formatDate(l.startDate)} <i class="bi bi-arrow-right mx-1 text-muted"></i> ${formatDate(l.endDate)}
                                    </td>
                                    <td class="fw-semibold tabular-nums">${formatCurrency(l.baseRent)}</td>
                                    <td>${renderLeaseBadge(l.status)}</td>
                                    <td>
                                        <div class="d-flex flex-wrap gap-1">
                                            ${(l.allowedActions || []).map(act => `
                                                <span class="badge bg-secondary-subtle text-secondary border font-monospace" style="font-size: 0.65rem;">
                                                    ${act}
                                                </span>
                                            `).join('') || '<span class="text-muted small">None</span>'}
                                        </div>
                                    </td>
                                    <td>
                                        <a href="#/leases/${l.id}" class="btn btn-sm btn-outline-primary">
                                            <i class="bi bi-box-arrow-up-right me-1"></i>Manage
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
        toast.error('Failed to load leases: ' + err.message);
    }
}

async function initCreateLeaseModal() {
    const openBtn = document.getElementById('openNewLeaseBtn');
    const modalEl = document.getElementById('createLeaseModal');
    const form = document.getElementById('createLeaseForm');
    const unitSelect = document.getElementById('leaseUnitSelect');
    const searchBtn = document.getElementById('searchTenantBtn');
    const searchInput = document.getElementById('tenantSearchEmail');
    const lookupResult = document.getElementById('tenantLookupResult');
    const tenantIdInput = document.getElementById('selectedTenantId');
    const saveBtn = document.getElementById('saveLeaseBtn');

    if (!modalEl) return;
    const bsModal = new window.bootstrap.Modal(modalEl);

    if (openBtn) {
        openBtn.onclick = async () => {
            form.reset();
            clearFormErrors(form);
            lookupResult.innerHTML = '';
            tenantIdInput.value = '';

            // Default dates: today and 1 year later
            const today = new Date().toISOString().split('T')[0];
            const nextYear = new Date(Date.now() + 365 * 86400000).toISOString().split('T')[0];
            form.startDate.value = today;
            form.endDate.value = nextYear;

            bsModal.show();

            // Populate Units
            try {
                const props = await api.get('/api/v1/properties', { size: 50 });
                const propList = props.content || [];
                let unitsHtml = '<option value="">-- Choose Unit --</option>';

                for (const p of propList) {
                    const units = await api.get(`/api/v1/properties/${p.id}/units`);
                    if (units && units.length > 0) {
                        unitsHtml += `<optgroup label="${escapeHtml(p.name)}">`;
                        units.forEach(u => {
                            unitsHtml += `<option value="${u.id}">${escapeHtml(p.name)} - Unit ${escapeHtml(u.unitNumber)} (${u.status})</option>`;
                        });
                        unitsHtml += `</optgroup>`;
                    }
                }
                unitSelect.innerHTML = unitsHtml;
            } catch (err) {
                unitSelect.innerHTML = '<option value="">Failed to load units</option>';
            }
        };
    }

    if (searchBtn) {
        searchBtn.onclick = async () => {
            const email = searchInput.value.trim();
            if (!email) {
                toast.warning('Enter a tenant email to search.');
                return;
            }

            lookupResult.innerHTML = '<span class="text-muted"><span class="spinner-border spinner-border-sm me-1"></span>Searching...</span>';

            try {
                const tenant = await api.get('/api/v1/users/tenants', { email });
                tenantIdInput.value = tenant.id;
                lookupResult.innerHTML = `
                    <div class="alert alert-success py-2 px-3 mb-0 d-flex align-items-center justify-content-between">
                        <div>
                            <strong><i class="bi bi-person-check me-1"></i>${escapeHtml(tenant.fullName)}</strong>
                            <div class="text-muted">${escapeHtml(tenant.email)} ${tenant.phone ? `• ${escapeHtml(tenant.phone)}` : ''}</div>
                        </div>
                        <span class="badge bg-success">Found</span>
                    </div>
                `;
            } catch (err) {
                tenantIdInput.value = '';
                lookupResult.innerHTML = `
                    <div class="alert alert-danger py-2 px-3 mb-0">
                        <i class="bi bi-exclamation-triangle me-1"></i>No registered tenant found with email: <strong>${escapeHtml(email)}</strong>.
                    </div>
                `;
            }
        };
    }

    if (form) {
        form.onsubmit = async (e) => {
            e.preventDefault();
            clearFormErrors(form);

            const unitId = form.unitId.value;
            const tenantId = tenantIdInput.value;
            const startDate = form.startDate.value;
            const endDate = form.endDate.value;
            const baseRent = form.baseRent.value;
            const securityDeposit = form.securityDeposit.value;

            if (!unitId || !tenantId) {
                toast.warning('Please select a unit and assign a valid tenant.');
                return;
            }
            if (new Date(endDate) <= new Date(startDate)) {
                toast.warning('End date must be strictly after start date.');
                return;
            }

            saveBtn.disabled = true;
            saveBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Creating...';

            try {
                const created = await api.post('/api/v1/leases', {
                    unitId: Number(unitId),
                    tenantId: Number(tenantId),
                    startDate,
                    endDate,
                    baseRent: Number(baseRent),
                    securityDeposit: Number(securityDeposit)
                });

                toast.success('Lease agreement created in DRAFT status.');
                bsModal.hide();
                window.location.hash = `#/leases/${created.id}`;
            } catch (err) {
                if (err.errors) {
                    setFormErrors(form, err.errors);
                } else {
                    toast.error(err.message || 'Failed to create lease agreement.');
                }
            } finally {
                saveBtn.disabled = false;
                saveBtn.innerHTML = 'Create Lease (Draft)';
            }
        };
    }
}

