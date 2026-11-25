/**
 * Paginated Data Table Component
 */

import { escapeHtml } from './form.js';
import { renderEmptyState } from './empty.js';

export function renderDataTable({
    columns = [],
    data = [],
    pagination = null,
    onPageChange = null,
    emptyMessage = 'No records found.'
}) {
    if (!data || data.length === 0) {
        return renderEmptyState({ description: emptyMessage });
    }

    const thead = columns.map(c => `<th class="${c.className || ''}">${c.label}</th>`).join('');

    const rows = data.map(row => {
        const cells = columns.map(col => {
            let val = '';
            if (typeof col.render === 'function') {
                val = col.render(row);
            } else if (col.key) {
                val = escapeHtml(row[col.key]);
            }
            return `<td class="${col.className || ''}">${val}</td>`;
        }).join('');
        return `<tr>${cells}</tr>`;
    }).join('');

    let paginationHtml = '';
    if (pagination && pagination.totalPages > 1) {
        const current = pagination.page;
        const total = pagination.totalPages;

        paginationHtml = `
            <div class="d-flex justify-content-between align-items-center mt-3 pt-3 border-top">
                <div class="small text-muted">
                    Showing page <strong>${current + 1}</strong> of <strong>${total}</strong> (${pagination.totalElements} total)
                </div>
                <nav aria-label="Table pagination">
                    <ul class="pagination pagination-sm mb-0">
                        <li class="page-item ${current === 0 ? 'disabled' : ''}">
                            <button class="page-link" data-page="${current - 1}" ${current === 0 ? 'disabled' : ''}>Previous</button>
                        </li>
                        <li class="page-item ${current >= total - 1 ? 'disabled' : ''}">
                            <button class="page-link" data-page="${current + 1}" ${current >= total - 1 ? 'disabled' : ''}>Next</button>
                        </li>
                    </ul>
                </nav>
            </div>
        `;
    }

    return `
        <div class="card p-3">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>${thead}</tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
            ${paginationHtml}
        </div>
    `;
}

