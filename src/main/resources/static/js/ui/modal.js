/**
 * Accessible Modal and Confirmation Dialogs
 */

class ModalManager {
    confirm({
        title = 'Confirm Action',
        message = 'Are you sure you want to proceed?',
        confirmText = 'Confirm',
        cancelText = 'Cancel',
        isDanger = false
    }) {
        return new Promise((resolve) => {
            const modalEl = document.createElement('div');
            modalEl.className = 'modal fade show';
            modalEl.style.display = 'block';
            modalEl.style.backgroundColor = 'rgba(0, 0, 0, 0.5)';
            modalEl.setAttribute('role', 'dialog');
            modalEl.setAttribute('aria-modal', 'true');

            modalEl.innerHTML = `
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content border-0 shadow-lg">
                        <div class="modal-header">
                            <h5 class="modal-title font-semibold">${title}</h5>
                            <button type="button" class="btn-close" aria-label="Close" data-action="cancel"></button>
                        </div>
                        <div class="modal-body py-4">
                            <p class="mb-0 text-secondary">${message}</p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary" data-action="cancel">${cancelText}</button>
                            <button type="button" class="btn ${isDanger ? 'btn-danger' : 'btn-primary'}" data-action="confirm">${confirmText}</button>
                        </div>
                    </div>
                </div>
            `;

            const close = (result) => {
                modalEl.remove();
                resolve(result);
            };

            modalEl.querySelectorAll('[data-action="cancel"]').forEach(btn => {
                btn.onclick = () => close(false);
            });

            modalEl.querySelector('[data-action="confirm"]').onclick = () => close(true);

            modalEl.onclick = (e) => {
                if (e.target === modalEl) close(false);
            };

            document.body.appendChild(modalEl);
        });
    }
}

export const modal = new ModalManager();

