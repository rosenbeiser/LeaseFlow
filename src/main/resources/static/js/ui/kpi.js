/**
 * KPI Metric Card Component
 */

export function renderKpiCard({ title, value, icon, isCurrency = false, subtitle = '' }) {
    const formatted = isCurrency
        ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value || 0)
        : (value !== null && value !== undefined ? Number(value).toLocaleString() : '0');

    return `
        <div class="card kpi-card">
            <div class="kpi-title">${title}</div>
            <div class="kpi-value tabular-nums">${formatted}</div>
            ${subtitle ? `<div class="small text-muted mt-1">${subtitle}</div>` : ''}
            <div class="kpi-icon">
                <i class="bi ${icon}"></i>
            </div>
        </div>
    `;
}

