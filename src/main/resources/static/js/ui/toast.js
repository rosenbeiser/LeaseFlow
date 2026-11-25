/**
 * Accessible Toast Notifications Component
 */

class ToastManager {
    constructor() {
        this.container = null;
        this.init();
    }

    init() {
        if (!this.container) {
            let el = document.getElementById('lf-toast-container');
            if (!el) {
                el = document.createElement('div');
                el.id = 'lf-toast-container';
                el.className = 'toast-container position-fixed bottom-0 end-0 p-3';
                el.style.zIndex = '9999';
                el.setAttribute('aria-live', 'polite');
                el.setAttribute('aria-atomic', 'true');
                document.body.appendChild(el);
            }
            this.container = el;
        }
    }

    show(message, type = 'info', duration = 4000) {
        this.init();

        const toastEl = document.createElement('div');
        toastEl.className = `toast align-items-center text-bg-${type} border-0 show shadow-lg mb-2`;
        toastEl.setAttribute('role', 'alert');
        toastEl.setAttribute('aria-live', 'assertive');
        toastEl.setAttribute('aria-atomic', 'true');

        const flexDiv = document.createElement('div');
        flexDiv.className = 'd-flex';

        const bodyDiv = document.createElement('div');
        bodyDiv.className = 'toast-body d-flex align-items-center gap-2';

        const icon = document.createElement('i');
        if (type === 'success') icon.className = 'bi bi-check-circle-fill fs-5';
        else if (type === 'danger') icon.className = 'bi bi-exclamation-octagon-fill fs-5';
        else if (type === 'warning') icon.className = 'bi bi-exclamation-triangle-fill fs-5';
        else icon.className = 'bi bi-info-circle-fill fs-5';

        const textSpan = document.createElement('span');
        textSpan.textContent = message;

        bodyDiv.appendChild(icon);
        bodyDiv.appendChild(textSpan);

        const closeBtn = document.createElement('button');
        closeBtn.type = 'button';
        closeBtn.className = 'btn-close btn-close-white me-2 m-auto';
        closeBtn.setAttribute('aria-label', 'Close');
        closeBtn.onclick = () => toastEl.remove();

        flexDiv.appendChild(bodyDiv);
        flexDiv.appendChild(closeBtn);
        toastEl.appendChild(flexDiv);

        this.container.appendChild(toastEl);

        if (duration > 0) {
            setTimeout(() => {
                if (toastEl.parentNode) {
                    toastEl.classList.remove('show');
                    setTimeout(() => toastEl.remove(), 250);
                }
            }, duration);
        }
    }

    success(msg, duration) { this.show(msg, 'success', duration); }
    error(msg, duration)   { this.show(msg, 'danger', duration); }
    warning(msg, duration) { this.show(msg, 'warning', duration); }
    info(msg, duration)    { this.show(msg, 'info', duration); }
}

export const toast = new ToastManager();

