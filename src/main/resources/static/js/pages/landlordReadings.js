/**
 * Landlord Meter Readings Management Module
 */

import { api } from '../api.js';
import { renderLandlordLayout, bindLayoutEvents } from '../layout.js';
import { renderSkeletonTable } from '../ui/skeleton.js';
import { renderEmptyState } from '../ui/empty.js';
import { escapeHtml, formatDate, setFormErrors, clearFormErrors } from '../ui/form.js';
import { toast } from '../ui/toast.js';

let selectedUnitId = null;

export async function renderLandlordReadings() {
    const appEl = document.getElementById('app');
    appEl.innerHTML = renderLandlordLayout(`
        <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">Meter Readings</h4>
                <p class="text-muted mb-0 small">Record consumption for electricity, water, and gas meters</p>
            </div>
            <button class="btn btn-primary" id="openAddReadingBtn">
                <i class="bi bi-speedometer2 me-1"></i>Record New Reading
            </button>
        </div>

        <div class="row g-4">
            <!-- Left: Unit selector -->
            <div class="col-md-4">
                <div class="card h-100">
                    <div class="card-header fw-semibold">Select Rental Unit</div>
                    <div class="card-body p-2">
                        <div class="list-group list-group-flush" id="unitSelectionList">
                            <div class="p-3 text-muted small text-center">Loading units...</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Right: Readings history for selected unit -->
            <div class="col-md-8">
                <div class="card h-100">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <span class="fw-semibold" id="readingHistoryTitle">Unit Readings History</span>
                        <span class="badge bg-body-secondary text-secondary border" id="readingCountBadge">0 records</span>
                    </div>
                    <div class="card-body p-0" id="readingsHistoryContainer">
                        ${renderSkeletonTable(5, 5)}
                    </div>
                </div>
            </div>
        </div>

        <!-- Record Reading Modal -->
        <div class="modal fade" id="recordReadingModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow-lg">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold">Record Meter Reading</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <form id="recordReadingForm" novalidate>
                        <div class="modal-body p-4">
                            <div class="mb-3">
                                <label class="form-label fw-semibold">Rental Unit</label>
                                <select class="form-select" name="unitId" id="modalUnitSelect" required></select>
                            </div>

                            <div class="mb-3">
                                <label class="form-label fw-semibold">Utility Type</label>
                                <select class="form-select" name="utilityType" id="modalUtilityTypeSelect" required>
                                    <option value="ELECTRICITY">Electricity (kWh)</option>
                                    <option value="WATER">Water (m³)</option>
                                    <option value="GAS">Gas (Commercial only)</option>
                                </select>
                            </div>

                            <div class="row g-3 mb-3">
                                <div class="col-6">
                                    <label class="form-label fw-semibold">Previous Reading</label>
                                    <input type="number" step="0.01" min="0" class="form-control tabular-nums" id="modalPrevReading" name="previousReading" placeholder="Auto / 0.00">
                                    <div class="form-text small">Derived automatically if blank.</div>
                                </div>
                                <div class="col-6">
                                    <label class="form-label fw-semibold">Current Reading</label>
                                    <input type="number" step="0.01" min="0" class="form-control tabular-nums" id="modalCurrentReading" name="currentReading" required placeholder="0.00">
                                </div>
                            </div>

                            <!-- Live Consumption Preview -->
                            <div class="p-3 rounded bg-body-tertiary border mb-3">
                                <div class="d-flex justify-content-between align-items-center">
                                    <span class="small fw-semibold text-muted">Calculated Net Consumption:</span>
                                    <span class="fw-bold fs-5 text-primary tabular-nums" id="liveConsumptionDisplay">0.00 units</span>
                                </div>
                            </div>

                            <div class="mb-3">
                                <label class="form-label fw-semibold">Reading Date</label>
                                <input type="date" class="form-control" name="readingDate" id="modalReadingDate" required>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="submit" class="btn btn-primary" id="saveReadingBtn">Save Reading</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    `, 'readings');
    bindLayoutEvents();

    initRecordReadingModal();
    await loadUnitsAndReadings();
}

async function loadUnitsAndReadings() {
    const unitListEl = document.getElementById('unitSelectionList');
    if (!unitListEl) return;

    try {
        const props = await api.get('/api/v1/properties', { size: 50 });
        const propList = props.content || [];

        const allUnits = [];
        for (const p of propList) {
            const units = await api.get(`/api/v1/properties/${p.id}/units`);
            units.forEach(u => allUnits.push({ ...u, propertyName: p.name }));
        }

        if (allUnits.length === 0) {
            unitListEl.innerHTML = '<div class="p-3 text-muted small text-center">No units registered. Add a property first.</div>';
            document.getElementById('readingsHistoryContainer').innerHTML = renderEmptyState({
                title: 'No Units Available',
                description: 'You need properties with rental units before taking meter readings.',
                actionText: 'Go to Properties',
                actionHash: '#/landlord/properties'
            });
            return;
        }

        unitListEl.innerHTML = allUnits.map((u, idx) => `
            <button type="button" class="list-group-item list-group-item-action ${idx === 0 ? 'active' : ''} unit-select-btn" data-unit-id="${u.id}" data-unit-name="${escapeHtml(u.propertyName)} - Unit ${escapeHtml(u.unitNumber)}">
                <div class="d-flex justify-content-between align-items-center">
                    <span class="fw-semibold">${escapeHtml(u.propertyName)}</span>
                    <span class="badge ${idx === 0 ? 'bg-light text-dark' : 'bg-primary-subtle text-primary'}">Unit ${escapeHtml(u.unitNumber)}</span>
                </div>
                <small class="opacity-75">Status: ${escapeHtml(u.status)}</small>
            </button>
        `).join('');

        selectedUnitId = allUnits[0].id;
        document.getElementById('readingHistoryTitle').textContent = `${allUnits[0].propertyName} - Unit ${allUnits[0].unitNumber}`;

        unitListEl.querySelectorAll('.unit-select-btn').forEach(btn => {
            btn.onclick = async () => {
                unitListEl.querySelectorAll('.unit-select-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                selectedUnitId = btn.getAttribute('data-unit-id');
                document.getElementById('readingHistoryTitle').textContent = btn.getAttribute('data-unit-name');
                await loadUnitReadings(selectedUnitId);
            };
        });

        await loadUnitReadings(selectedUnitId);

    } catch (err) {
        toast.error('Failed to load units: ' + err.message);
    }
}

async function loadUnitReadings(unitId) {
    const container = document.getElementById('readingsHistoryContainer');
    const badge = document.getElementById('readingCountBadge');
    if (!container) return;

    container.innerHTML = renderSkeletonTable(4, 5);

    try {
        const res = await api.get('/api/v1/meter-readings', { unitId, size: 50 });
        const readings = res.content || [];
        if (badge) badge.textContent = `${readings.length} records`;

        if (readings.length === 0) {
            container.innerHTML = `
                <div class="p-4 text-center text-muted">
                    <i class="bi bi-speedometer2 fs-2 text-secondary opacity-50 mb-2 d-block"></i>
                    No meter readings recorded for this unit yet.
                </div>
            `;
            return;
        }

        container.innerHTML = `
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th>Date</th>
                            <th>Utility</th>
                            <th>Previous</th>
                            <th>Current</th>
                            <th>Consumed</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${readings.map(r => `
                            <tr>
                                <td>${formatDate(r.readingDate)}</td>
                                <td>
                                    <span class="badge bg-light text-dark border">
                                        <i class="bi bi-${r.utilityType === 'ELECTRICITY' ? 'lightning' : (r.utilityType === 'WATER' ? 'droplet' : 'fire')} me-1"></i>
                                        ${escapeHtml(r.utilityType)}
                                    </span>
                                </td>
                                <td class="tabular-nums text-muted">${r.previousReading}</td>
                                <td class="tabular-nums fw-semibold">${r.currentReading}</td>
                                <td class="tabular-nums fw-bold text-primary">
                                    +${r.consumption} units
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (err) {
        container.innerHTML = `<div class="p-3 text-danger">${escapeHtml(err.message)}</div>`;
    }
}

function initRecordReadingModal() {
    const openBtn = document.getElementById('openAddReadingBtn');
    const modalEl = document.getElementById('recordReadingModal');
    const form = document.getElementById('recordReadingForm');
    const unitSelect = document.getElementById('modalUnitSelect');
    const prevInput = document.getElementById('modalPrevReading');
    const currInput = document.getElementById('modalCurrentReading');
    const liveDisplay = document.getElementById('liveConsumptionDisplay');
    const dateInput = document.getElementById('modalReadingDate');
    const saveBtn = document.getElementById('saveReadingBtn');

    if (!modalEl) return;
    const bsModal = new window.bootstrap.Modal(modalEl);

    // Live consumption update listener
    const updateLiveConsumption = () => {
        const prev = parseFloat(prevInput.value) || 0;
        const curr = parseFloat(currInput.value) || 0;
        const net = Math.max(0, curr - prev);
        liveDisplay.textContent = `${net.toFixed(2)} units`;
    };

    prevInput.oninput = updateLiveConsumption;
    currInput.oninput = updateLiveConsumption;

    if (openBtn) {
        openBtn.onclick = async () => {
            form.reset();
            clearFormErrors(form);
            dateInput.value = new Date().toISOString().split('T')[0];
            liveDisplay.textContent = '0.00 units';

            bsModal.show();

            // Populate units in select
            try {
                const props = await api.get('/api/v1/properties', { size: 50 });
                const propList = props.content || [];
                let html = '';
                for (const p of propList) {
                    const units = await api.get(`/api/v1/properties/${p.id}/units`);
                    if (units && units.length > 0) {
                        html += `<optgroup label="${escapeHtml(p.name)}">`;
                        units.forEach(u => {
                            const isSel = selectedUnitId == u.id ? 'selected' : '';
                            html += `<option value="${u.id}" ${isSel}>${escapeHtml(p.name)} - Unit ${escapeHtml(u.unitNumber)}</option>`;
                        });
                        html += `</optgroup>`;
                    }
                }
                unitSelect.innerHTML = html;
            } catch {
                unitSelect.innerHTML = '<option value="">Failed to load units</option>';
            }
        };
    }

    if (form) {
        form.onsubmit = async (e) => {
            e.preventDefault();
            clearFormErrors(form);

            const unitId = form.unitId.value;
            const utilityType = form.utilityType.value;
            const previousReading = form.previousReading.value ? Number(form.previousReading.value) : null;
            const currentReading = Number(form.currentReading.value);
            const readingDate = form.readingDate.value;

            if (!unitId || isNaN(currentReading) || !readingDate) {
                toast.warning('Please complete all required reading fields.');
                return;
            }

            if (previousReading !== null && currentReading < previousReading) {
                toast.error('Current reading cannot be lower than previous reading.');
                return;
            }

            saveBtn.disabled = true;
            saveBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Saving...';

            try {
                await api.post('/api/v1/meter-readings', {
                    unitId: Number(unitId),
                    utilityType,
                    previousReading,
                    currentReading,
                    readingDate
                });

                toast.success('Meter reading saved successfully.');
                bsModal.hide();
                await loadUnitReadings(selectedUnitId || unitId);
            } catch (err) {
                if (err.errors) {
                    setFormErrors(form, err.errors);
                } else {
                    toast.error(err.message || 'Failed to record meter reading.');
                }
            } finally {
                saveBtn.disabled = false;
                saveBtn.innerHTML = 'Save Reading';
            }
        };
    }
}

