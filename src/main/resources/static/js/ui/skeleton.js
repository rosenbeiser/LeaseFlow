/**
 * Skeleton Loading Component
 */

export function renderSkeletonTable(rows = 5, cols = 4) {
    let rowsHtml = '';
    for (let r = 0; r < rows; r++) {
        let colsHtml = '';
        for (let c = 0; c < cols; c++) {
            colsHtml += `<td><div class="skeleton skeleton-text" style="width: ${60 + (c * 10)}%;"></div></td>`;
        }
        rowsHtml += `<tr>${colsHtml}</tr>`;
    }

    return `
        <div class="card p-3">
            <div class="table-responsive">
                <table class="table align-middle">
                    <tbody>${rowsHtml}</tbody>
                </table>
            </div>
        </div>
    `;
}

export function renderSkeletonCards(count = 4) {
    let html = '<div class="row g-3 mb-4">';
    for (let i = 0; i < count; i++) {
        html += `
            <div class="col-sm-6 col-xl-3">
                <div class="card p-3 skeleton skeleton-card"></div>
            </div>
        `;
    }
    html += '</div>';
    return html;
}

