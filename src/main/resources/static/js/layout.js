/**
 * Application Layout Shell & Theme Manager
 */

import { auth } from './auth.js';
import { escapeHtml } from './ui/form.js';

export class ThemeManager {
    static init() {
        const saved = localStorage.getItem('lf_theme');
        if (saved) {
            document.documentElement.setAttribute('data-bs-theme', saved);
        } else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            document.documentElement.setAttribute('data-bs-theme', 'dark');
        }
    }

    static toggle() {
        const current = document.documentElement.getAttribute('data-bs-theme') || 'light';
        const next = current === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-bs-theme', next);
        localStorage.setItem('lf_theme', next);
        this.updateIcons();
    }

    static updateIcons() {
        const isDark = document.documentElement.getAttribute('data-bs-theme') === 'dark';
        document.querySelectorAll('.theme-toggle-icon').forEach(icon => {
            icon.className = `bi bi-${isDark ? 'sun' : 'moon-stars'} theme-toggle-icon`;
        });
    }
}

export function renderLandlordLayout(contentHtml, activeRoute) {
    const user = auth.getUser() || { fullName: 'Landlord User', email: '' };

    return `
        <div class="app-wrapper">
            <!-- Sidebar -->
            <aside class="app-sidebar shadow-sm" id="appSidebar">
                <div class="d-flex align-items-center justify-content-between p-3 border-bottom" style="height: var(--lf-header-height);">
                    <a href="#/landlord/dashboard" class="d-flex align-items-center gap-2 text-decoration-none text-body">
                        <i class="bi bi-building-gear fs-4 text-primary"></i>
                        <span class="fs-5 fw-bold letter-spacing-tight">LeaseFlow</span>
                    </a>
                    <button class="btn btn-sm btn-outline-secondary d-lg-none" id="closeSidebarBtn" aria-label="Close Sidebar">
                        <i class="bi bi-x-lg"></i>
                    </button>
                </div>

                <div class="p-3 flex-grow-1 overflow-auto">
                    <div class="small text-muted text-uppercase fw-semibold px-2 mb-2">Management</div>
                    <nav class="nav flex-column">
                        <a href="#/landlord/dashboard" class="nav-link-item ${activeRoute === 'dashboard' ? 'active' : ''}">
                            <i class="bi bi-grid-1x2"></i><span>Dashboard</span>
                        </a>
                        <a href="#/landlord/properties" class="nav-link-item ${activeRoute === 'properties' ? 'active' : ''}">
                            <i class="bi bi-building"></i><span>Properties</span>
                        </a>
                        <a href="#/landlord/leases" class="nav-link-item ${activeRoute === 'leases' ? 'active' : ''}">
                            <i class="bi bi-file-earmark-text"></i><span>Leases</span>
                        </a>
                        <a href="#/landlord/readings" class="nav-link-item ${activeRoute === 'readings' ? 'active' : ''}">
                            <i class="bi bi-speedometer2"></i><span>Meter Readings</span>
                        </a>
                        <a href="#/landlord/invoices" class="nav-link-item ${activeRoute === 'invoices' ? 'active' : ''}">
                            <i class="bi bi-receipt"></i><span>Invoices</span>
                        </a>
                    </nav>
                </div>

                <!-- User Footer -->
                <div class="p-3 border-top mt-auto bg-body-tertiary">
                    <div class="d-flex align-items-center justify-content-between">
                        <div class="d-flex align-items-center gap-2 overflow-hidden">
                            <div class="rounded-circle bg-primary text-white d-flex align-items-center justify-content-center fw-bold" style="width: 36px; height: 36px; flex-shrink: 0;">
                                ${escapeHtml(user.fullName.charAt(0).toUpperCase())}
                            </div>
                            <div class="text-truncate">
                                <div class="fw-semibold text-truncate small">${escapeHtml(user.fullName)}</div>
                                <div class="text-muted text-truncate" style="font-size: 0.75rem;">${escapeHtml(user.email)}</div>
                            </div>
                        </div>
                        <button class="btn btn-sm btn-outline-danger" id="logoutBtn" title="Logout" aria-label="Logout">
                            <i class="bi bi-box-arrow-right"></i>
                        </button>
                    </div>
                </div>
            </aside>

            <!-- Main Content Area -->
            <div class="app-main">
                <header class="app-header">
                    <div class="d-flex align-items-center gap-2">
                        <button class="btn btn-sm btn-outline-secondary d-lg-none" id="openSidebarBtn" aria-label="Open Navigation">
                            <i class="bi bi-list fs-5"></i>
                        </button>
                        <h5 class="mb-0 fw-semibold text-capitalize">${activeRoute}</h5>
                    </div>

                    <div class="d-flex align-items-center gap-2">
                        <!-- Theme Toggle Button -->
                        <button class="btn btn-sm btn-outline-secondary rounded-circle p-2 d-flex align-items-center justify-content-center" id="themeToggleBtn" style="width: 36px; height: 36px;" title="Toggle Dark/Light Mode" aria-label="Toggle theme">
                            <i class="bi bi-moon-stars theme-toggle-icon"></i>
                        </button>
                    </div>
                </header>

                <main class="app-content fade-in">
                    ${contentHtml}
                </main>
            </div>
        </div>
    `;
}

export function renderTenantLayout(contentHtml, activeRoute) {
    const user = auth.getUser() || { fullName: 'Tenant User', email: '' };

    return `
        <div class="app-wrapper tenant-layout">
            <div class="app-main">
                <!-- Top Navbar for Tenant -->
                <header class="app-header">
                    <div class="d-flex align-items-center gap-3">
                        <a href="#/tenant/dashboard" class="d-flex align-items-center gap-2 text-decoration-none text-body">
                            <i class="bi bi-building-gear fs-4 text-primary"></i>
                            <span class="fs-5 fw-bold letter-spacing-tight">LeaseFlow</span>
                        </a>

                        <nav class="nav d-none d-md-flex align-items-center gap-1 ms-3">
                            <a href="#/tenant/dashboard" class="nav-link py-1 px-3 rounded ${activeRoute === 'dashboard' ? 'fw-bold text-primary bg-primary-subtle' : 'text-secondary'}">Dashboard</a>
                            <a href="#/tenant/bills" class="nav-link py-1 px-3 rounded ${activeRoute === 'bills' ? 'fw-bold text-primary bg-primary-subtle' : 'text-secondary'}">My Bills</a>
                            <a href="#/tenant/payments" class="nav-link py-1 px-3 rounded ${activeRoute === 'payments' ? 'fw-bold text-primary bg-primary-subtle' : 'text-secondary'}">Payment History</a>
                        </nav>
                    </div>

                    <div class="d-flex align-items-center gap-2">
                        <button class="btn btn-sm btn-outline-secondary rounded-circle p-2 d-flex align-items-center justify-content-center" id="themeToggleBtn" style="width: 36px; height: 36px;" title="Toggle theme" aria-label="Toggle theme">
                            <i class="bi bi-moon-stars theme-toggle-icon"></i>
                        </button>

                        <div class="d-flex align-items-center gap-2 ms-2">
                            <span class="small fw-semibold d-none d-sm-inline">${escapeHtml(user.fullName)}</span>
                            <button class="btn btn-sm btn-outline-danger" id="logoutBtn" title="Logout" aria-label="Logout">
                                <i class="bi bi-box-arrow-right"></i>
                            </button>
                        </div>
                    </div>
                </header>

                <main class="app-content container-xl py-4 fade-in">
                    ${contentHtml}
                </main>
            </div>
        </div>
    `;
}

export function bindLayoutEvents() {
    ThemeManager.updateIcons();

    const themeBtn = document.getElementById('themeToggleBtn');
    if (themeBtn) {
        themeBtn.onclick = () => ThemeManager.toggle();
    }

    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.onclick = () => auth.logout();
    }

    const sidebar = document.getElementById('appSidebar');
    const openBtn = document.getElementById('openSidebarBtn');
    const closeBtn = document.getElementById('closeSidebarBtn');

    if (sidebar && openBtn) {
        openBtn.onclick = () => sidebar.classList.add('show');
    }
    if (sidebar && closeBtn) {
        closeBtn.onclick = () => sidebar.classList.remove('show');
    }
}

