/**
 * Designed Empty State Component with Inline SVG
 */

export function renderEmptyState({
    title = 'No Data Found',
    description = 'There are no items recorded yet.',
    actionText = '',
    actionHash = '',
    actionBtnId = ''
}) {
    return `
        <div class="card p-5 text-center my-3">
            <div class="mb-3 d-flex justify-content-center">
                <svg width="72" height="72" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="text-secondary opacity-50">
                    <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
                    <line x1="12" y1="11" x2="12" y2="17"></line>
                    <line x1="9" y1="14" x2="15" y2="14"></line>
                </svg>
            </div>
            <h5 class="fw-semibold mb-1">${title}</h5>
            <p class="text-muted mb-3 mx-auto" style="max-width: 380px;">${description}</p>
            ${actionText && actionHash ? `
                <div>
                    <a href="${actionHash}" class="btn btn-primary btn-sm">
                        <i class="bi bi-plus-lg me-1"></i>${actionText}
                    </a>
                </div>
            ` : ''}
            ${actionText && actionBtnId ? `
                <div>
                    <button type="button" id="${actionBtnId}" class="btn btn-primary btn-sm">
                        <i class="bi bi-plus-lg me-1"></i>${actionText}
                    </button>
                </div>
            ` : ''}
        </div>
    `;
}

