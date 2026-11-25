/**
 * Vertical Lease Stepper / Timeline Component
 */

import { escapeHtml, formatDateTime } from './form.js';
import { renderLeaseBadge } from './badge.js';

export function renderLeaseStepper(historyList = [], currentStatus = 'DRAFT') {
    if (!historyList || historyList.length === 0) {
        return `
            <div class="p-3 text-muted small">
                Current status: ${renderLeaseBadge(currentStatus)}
            </div>
        `;
    }

    const itemsHtml = historyList.map(h => {
        const isCurrent = h.toStatus === currentStatus;
        return `
            <div class="step-item ${isCurrent ? 'active' : ''} ${h.toStatus === 'SETTLED' ? 'settled' : ''}">
                <div class="step-bullet"></div>
                <div class="d-flex justify-content-between align-items-baseline mb-1">
                    <span class="fw-semibold">${renderLeaseBadge(h.toStatus)}</span>
                    <span class="small text-muted">${formatDateTime(h.changedAt)}</span>
                </div>
                <div class="small text-secondary">
                    Transition from <strong>${escapeHtml(h.fromStatus)}</strong>
                    ${h.notes ? `<div class="mt-1 p-2 rounded bg-body-tertiary"><em>${escapeHtml(h.notes)}</em></div>` : ''}
                </div>
            </div>
        `;
    }).join('');

    return `
        <div class="lease-stepper">
            ${itemsHtml}
        </div>
    `;
}

