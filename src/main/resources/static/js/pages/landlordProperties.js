/**
 * Landlord Properties Management Module
 */

import { api } from '../api.js';
import { renderLandlordLayout, bindLayoutEvents } from '../layout.js';
import { renderSkeletonCards } from '../ui/skeleton.js';
import { renderEmptyState } from '../ui/empty.js';
import { renderUnitBadge } from '../ui/badge.js';
import { escapeHtml, setFormErrors, clearFormErrors } from '../ui/form.js';
import { toast } from '../ui/toast.js';

let propertiesList = [];

export async function renderLandlordProperties() {
    const appEl = document.getElementById('app');
    appEl.innerHTML = renderLandlordLayout(`
        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <h4 class="fw-bold mb-1">Properties</h4>
                <p class="text-muted mb-0 small">Manage your real estate assets, units, and occupancy</p>
            </div>
            <button class="btn btn-primary" id="openAddPropertyBtn">
                <i class="bi bi-plus-lg me-1"></i>Add Property
            </button>
        </div>

        <div id="propertiesContainer">
            ${renderSkeletonCards(3)}
        </div>

        <!-- Add Property Modal -->
        <div class="modal fade" id="addPropertyModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-lg modal-dialog-centered">
                <div class="modal-content border-0 shadow-lg">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold">Add New Property</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <form id="addPropertyForm" novalidate>
                        <div class="modal-body p-4">
                            <div class="row g-3 mb-3">
                                <div class="col-md-8">
                                    <label class="form-label fw-semibold">Property Name</label>
                                    <input type="text" class="form-control" name="name" required placeholder="e.g. Sunrise Heights">
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label fw-semibold">Property Type</label>
                                    <select class="form-select" name="propertyType" required>
                                        <option value="RESIDENTIAL">Residential</option>
                                        <option value="COMMERCIAL">Commercial</option>
                                    </select>
                                </div>
                                <div class="col-12">
                                    <label class="form-label fw-semibold">Address</label>
                                    <input type="text" class="form-control" name="address" required placeholder="Full street address, city, zip">
                                </div>
                            </div>

                            <div class="border-top pt-3 mt-3">
                                <div class="d-flex justify-content-between align-items-center mb-2">
                                    <h6 class="fw-bold mb-0">Initial Units</h6>
                                    <button type="button" class="btn btn-sm btn-outline-primary" id="addUnitRowBtn">
                                        <i class="bi bi-plus me-1"></i>Add Unit
                                    </button>
                                </div>
                                <div id="unitRowsContainer" class="d-flex flex-column gap-2">
                                    <!-- Dynamic Unit Rows appended here -->
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="submit" class="btn btn-primary" id="savePropertyBtn">Save Property</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>

        <!-- View Units Modal -->
        <div class="modal fade" id="viewUnitsModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow-lg">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold" id="viewUnitsTitle">Property Units</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body p-0">
                        <div class="table-responsive">
                            <table class="table table-hover align-middle mb-0">
                                <thead class="table-light">
                                    <tr>
                                        <th>Unit Number</th>
                                        <th>Floor</th>
                                        <th>Status</th>
                                    </tr>
                                </thead>
                                <tbody id="unitsTableBody"></tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `, 'properties');
    bindLayoutEvents();

    initAddPropertyModal();
    await loadProperties();
}

async function loadProperties() {
    const container = document.getElementById('propertiesContainer');
    if (!container) return;

    try {
        const res = await api.get('/api/v1/properties', { size: 50 });
        propertiesList = res.content || [];

        if (propertiesList.length === 0) {
            container.innerHTML = renderEmptyState({
                title: 'No Properties Added Yet',
                description: 'Start managing your units and leases by adding your first real estate asset.',
                actionText: 'Add Property',
                actionBtnId: 'emptyAddPropertyBtn'
            });
            const emptyBtn = document.getElementById('emptyAddPropertyBtn');
            if (emptyBtn) {
                emptyBtn.onclick = () => {
                    const modalEl = new window.bootstrap.Modal(document.getElementById('addPropertyModal'));
                    modalEl.show();
                };
            }
            return;
        }

        container.innerHTML = `
            <div class="row g-4">
                ${propertiesList.map(p => renderPropertyCard(p)).join('')}
            </div>
        `;

        // Bind "View Units" buttons
        container.querySelectorAll('[data-view-units]').forEach(btn => {
            btn.onclick = async () => {
                const propId = btn.getAttribute('data-view-units');
                await showUnitsModal(propId);
            };
        });

    } catch (err) {
        toast.error('Failed to load properties: ' + err.message);
    }
}

function renderPropertyCard(p) {
    const isCommercial = p.propertyType === 'COMMERCIAL';
    return `
        <div class="col-md-6 col-xl-4">
            <div class="card h-100 card-hover">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <span class="badge ${isCommercial ? 'bg-warning-subtle text-warning border border-warning' : 'bg-primary-subtle text-primary border border-primary'} text-uppercase small fw-semibold">
                            ${p.propertyType}
                        </span>
                        <small class="text-muted"><i class="bi bi-geo-alt me-1"></i>Property #${p.id}</small>
                    </div>
                    <h5 class="fw-bold mb-1">${escapeHtml(p.name)}</h5>
                    <p class="text-muted small mb-3">${escapeHtml(p.address)}</p>

                    <div class="d-flex align-items-center justify-content-between pt-3 border-top mt-auto">
                        <button class="btn btn-sm btn-outline-primary" data-view-units="${p.id}">
                            <i class="bi bi-door-open me-1"></i>View Units
                        </button>
                        <a href="#/landlord/leases" class="btn btn-sm btn-light border">
                            <i class="bi bi-file-text me-1"></i>Leases
                        </a>
                    </div>
                </div>
            </div>
        </div>
    `;
}

async function showUnitsModal(propertyId) {
    const prop = propertiesList.find(p => p.id == propertyId);
    const titleEl = document.getElementById('viewUnitsTitle');
    const tbody = document.getElementById('unitsTableBody');

    if (titleEl && prop) {
        titleEl.textContent = `${prop.name} — Units`;
    }

    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="3" class="text-center py-3"><div class="spinner-border spinner-border-sm text-primary"></div></td></tr>';
        const modalEl = new window.bootstrap.Modal(document.getElementById('viewUnitsModal'));
        modalEl.show();

        try {
            const units = await api.get(`/api/v1/properties/${propertyId}/units`);
            if (!units || units.length === 0) {
                tbody.innerHTML = '<tr><td colspan="3" class="text-center py-3 text-muted">No units registered for this property.</td></tr>';
                return;
            }

            tbody.innerHTML = units.map(u => `
                <tr>
                    <td class="fw-semibold">${escapeHtml(u.unitNumber)}</td>
                    <td>${escapeHtml(u.floor || 'G')}</td>
                    <td>${renderUnitBadge(u.status)}</td>
                </tr>
            `).join('');
        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="3" class="text-center py-3 text-danger">${escapeHtml(err.message)}</td></tr>`;
        }
    }
}

function initAddPropertyModal() {
    const openBtn = document.getElementById('openAddPropertyBtn');
    const modalEl = document.getElementById('addPropertyModal');
    const form = document.getElementById('addPropertyForm');
    const addUnitBtn = document.getElementById('addUnitRowBtn');
    const unitContainer = document.getElementById('unitRowsContainer');
    const saveBtn = document.getElementById('savePropertyBtn');

    if (!modalEl) return;
    const bsModal = new window.bootstrap.Modal(modalEl);

    if (openBtn) {
        openBtn.onclick = () => {
            form.reset();
            clearFormErrors(form);
            unitContainer.innerHTML = '';
            addUnitRow('101', '1');
            bsModal.show();
        };
    }

    if (addUnitBtn) {
        addUnitBtn.onclick = () => {
            const rowCount = unitContainer.children.length + 1;
            addUnitRow(`${rowCount}01`, `${rowCount}`);
        };
    }

    function addUnitRow(defaultNum = '', defaultFloor = '') {
        const row = document.createElement('div');
        row.className = 'row g-2 align-items-center unit-row';
        row.innerHTML = `
            <div class="col-6">
                <input type="text" class="form-control form-control-sm" name="unitNumber" required placeholder="Unit #" value="${defaultNum}">
            </div>
            <div class="col-4">
                <input type="text" class="form-control form-control-sm" name="floor" placeholder="Floor" value="${defaultFloor}">
            </div>
            <div class="col-2 text-end">
                <button type="button" class="btn btn-sm btn-outline-danger remove-unit-btn" title="Remove unit">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        `;

        row.querySelector('.remove-unit-btn').onclick = () => {
            if (unitContainer.children.length > 1) {
                row.remove();
            } else {
                toast.warning('A property must have at least one unit.');
            }
        };

        unitContainer.appendChild(row);
    }

    if (form) {
        form.onsubmit = async (e) => {
            e.preventDefault();
            clearFormErrors(form);

            const name = form.name.value.trim();
            const address = form.address.value.trim();
            const propertyType = form.propertyType.value;

            const unitRows = unitContainer.querySelectorAll('.unit-row');
            const units = [];
            unitRows.forEach(r => {
                const num = r.querySelector('[name="unitNumber"]').value.trim();
                const floor = r.querySelector('[name="floor"]').value.trim();
                if (num) {
                    units.push({ unitNumber: num, floor: floor || null });
                }
            });

            if (!name || !address) {
                toast.warning('Please enter property name and address.');
                return;
            }
            if (units.length === 0) {
                toast.warning('At least one unit is required.');
                return;
            }

            saveBtn.disabled = true;
            saveBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Saving...';

            try {
                await api.post('/api/v1/properties', {
                    name,
                    address,
                    propertyType,
                    units
                });

                toast.success('Property created successfully with ' + units.length + ' units!');
                bsModal.hide();
                await loadProperties();
            } catch (err) {
                if (err.errors) {
                    setFormErrors(form, err.errors);
                } else {
                    toast.error(err.message || 'Failed to create property.');
                }
            } finally {
                saveBtn.disabled = false;
                saveBtn.innerHTML = 'Save Property';
            }
        };
    }
}

