/**
 * LeaseFlow Frontend Main Entrypoint
 */

import { auth } from './auth.js';
import { router } from './router.js';
import { ThemeManager } from './layout.js';
import { renderLogin, initLogin } from './pages/login.js';
import { renderRegister, initRegister } from './pages/register.js';
import { renderLandlordDashboard } from './pages/landlordDashboard.js';
import { renderLandlordProperties } from './pages/landlordProperties.js';
import { renderLandlordLeases } from './pages/landlordLeases.js';
import { renderLeaseDetail } from './pages/leaseDetail.js';
import { renderLandlordReadings } from './pages/landlordReadings.js';
import { renderLandlordInvoices } from './pages/landlordInvoices.js';
import { renderInvoiceDetail } from './pages/invoiceDetail.js';
import { renderTenantDashboard } from './pages/tenantDashboard.js';
import { renderTenantBills } from './pages/tenantBills.js';
import { renderTenantPayments } from './pages/tenantPayments.js';

// Initialize Theme
ThemeManager.init();

// ── Auth Routes ─────────────────────────────────────────────────────────────
router.add('/login', () => {
    if (auth.isAuthenticated()) {
        if (auth.isLandlord() || auth.isAdmin()) {
            router.navigate('#/landlord/dashboard');
            return;
        } else if (auth.isTenant()) {
            router.navigate('#/tenant/dashboard');
            return;
        }
    }
    const app = document.getElementById('app');
    app.innerHTML = renderLogin();
    initLogin();
}, { title: 'Sign In' });

router.add('/register', () => {
    if (auth.isAuthenticated()) {
        if (auth.isLandlord() || auth.isAdmin()) {
            router.navigate('#/landlord/dashboard');
            return;
        } else if (auth.isTenant()) {
            router.navigate('#/tenant/dashboard');
            return;
        }
    }
    const app = document.getElementById('app');
    app.innerHTML = renderRegister();
    initRegister();
}, { title: 'Create Account' });

// ── Landlord Routes ─────────────────────────────────────────────────────────
router.add('/landlord/dashboard', () => renderLandlordDashboard(), {
    title: 'Landlord Dashboard',
    roles: ['ROLE_LANDLORD', 'ROLE_ADMIN']
});

router.add('/landlord/properties', () => renderLandlordProperties(), {
    title: 'Properties & Units',
    roles: ['ROLE_LANDLORD', 'ROLE_ADMIN']
});

router.add('/landlord/leases', () => renderLandlordLeases(), {
    title: 'Lease Agreements',
    roles: ['ROLE_LANDLORD', 'ROLE_ADMIN']
});

router.add('/landlord/readings', () => renderLandlordReadings(), {
    title: 'Meter Readings',
    roles: ['ROLE_LANDLORD', 'ROLE_ADMIN']
});

router.add('/landlord/invoices', () => renderLandlordInvoices(), {
    title: 'Invoices & Billing',
    roles: ['ROLE_LANDLORD', 'ROLE_ADMIN']
});

// ── Tenant Routes ───────────────────────────────────────────────────────────
router.add('/tenant/dashboard', () => renderTenantDashboard(), {
    title: 'Tenant Portal',
    roles: ['ROLE_TENANT', 'ROLE_ADMIN']
});

router.add('/tenant/bills', () => renderTenantBills(), {
    title: 'My Bills',
    roles: ['ROLE_TENANT', 'ROLE_ADMIN']
});

router.add('/tenant/payments', () => renderTenantPayments(), {
    title: 'Payment History',
    roles: ['ROLE_TENANT', 'ROLE_ADMIN']
});

// ── Shared Detail Routes ────────────────────────────────────────────────────
router.add('/leases/:id', (params) => renderLeaseDetail(params), {
    title: 'Lease Agreement',
    roles: ['ROLE_LANDLORD', 'ROLE_TENANT', 'ROLE_ADMIN']
});

router.add('/invoices/:id', (params) => renderInvoiceDetail(params), {
    title: 'Invoice Statement',
    roles: ['ROLE_LANDLORD', 'ROLE_TENANT', 'ROLE_ADMIN']
});

// Default catch-all root
if (!window.location.hash || window.location.hash === '#/') {
    if (auth.isAuthenticated()) {
        if (auth.isLandlord() || auth.isAdmin()) {
            window.location.hash = '#/landlord/dashboard';
        } else {
            window.location.hash = '#/tenant/dashboard';
        }
    } else {
        window.location.hash = '#/login';
    }
}

// Kick off initial route
router.handleRoute();
