/**
 * Form helpers, validation mapper, XSS escape, and formatting utils
 */

export function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

export function setFormErrors(formEl, errorMap) {
    clearFormErrors(formEl);
    if (!formEl || !errorMap) return;

    Object.entries(errorMap).forEach(([field, msg]) => {
        const input = formEl.querySelector(`[name="${field}"]`);
        if (input) {
            input.classList.add('is-invalid');
            let feedback = input.parentElement.querySelector('.invalid-feedback');
            if (!feedback) {
                feedback = document.createElement('div');
                feedback.className = 'invalid-feedback';
                input.parentElement.appendChild(feedback);
            }
            feedback.textContent = msg;
        }
    });
}

export function clearFormErrors(formEl) {
    if (!formEl) return;
    formEl.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    formEl.querySelectorAll('.invalid-feedback').forEach(el => el.textContent = '');
}

export function formatCurrency(amount) {
    if (amount === null || amount === undefined) return '$0.00';
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
    }).format(amount);
}

export function formatDate(dateStr) {
    if (!dateStr) return '—';
    try {
        const d = new Date(dateStr);
        return new Intl.DateTimeFormat('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        }).format(d);
    } catch {
        return dateStr;
    }
}

export function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '—';
    try {
        const d = new Date(dateTimeStr);
        return new Intl.DateTimeFormat('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        }).format(d);
    } catch {
        return dateTimeStr;
    }
}

