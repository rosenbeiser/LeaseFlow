/**
 * Login Page Module
 */

import { api } from '../api.js';
import { auth } from '../auth.js';
import { toast } from '../ui/toast.js';
import { clearFormErrors, setFormErrors } from '../ui/form.js';

export function renderLogin() {
    return `
        <div class="row min-vh-100 g-0">
            <!-- Left Branding Column -->
            <div class="col-lg-6 d-none d-lg-flex flex-column justify-content-between p-5 text-white" style="background: linear-gradient(135deg, #4338ca 0%, #1e1b4b 100%);">
                <div>
                    <div class="d-flex align-items-center gap-2 mb-4">
                        <i class="bi bi-building-gear fs-3 text-warning"></i>
                        <span class="fs-4 fw-bold letter-spacing-tight">LeaseFlow</span>
                    </div>
                    <div class="mt-5">
                        <span class="badge bg-white bg-opacity-10 text-white border border-white border-opacity-25 px-3 py-2 rounded-pill mb-3">Enterprise Ready</span>
                        <h1 class="display-5 fw-bold leading-tight">Smart Property Lease &amp; Utility Billing System</h1>
                        <p class="lead opacity-75 mt-3">
                            Automate progressive tiered tariffs, lease lifecycle state transitions, and itemized billing statements in one place.
                        </p>
                    </div>
                </div>
                <div class="small opacity-50">
                    &copy; ${new Date().getFullYear()} LeaseFlow Inc. Secure role-based property operations.
                </div>
            </div>

            <!-- Right Form Column -->
            <div class="col-lg-6 d-flex align-items-center justify-content-center p-4 p-md-5">
                <div class="w-100" style="max-width: 420px;">
                    <div class="text-center mb-4 d-lg-none">
                        <div class="d-inline-flex align-items-center gap-2 mb-2">
                            <i class="bi bi-building-gear fs-2 text-primary"></i>
                            <span class="fs-3 fw-bold">LeaseFlow</span>
                        </div>
                    </div>

                    <h2 class="fw-bold mb-1">Welcome back</h2>
                    <p class="text-muted mb-4">Sign in with your email and password to continue</p>

                    <form id="loginForm" novalidate>
                        <div class="mb-3">
                            <label for="email" class="form-label fw-semibold">Email address</label>
                            <input type="email" class="form-control form-control-lg" id="email" name="email" required placeholder="e.g. landlord1@leaseflow.dev" autocomplete="email">
                        </div>

                        <div class="mb-3">
                            <div class="d-flex justify-content-between align-items-center">
                                <label for="password" class="form-label fw-semibold mb-0">Password</label>
                            </div>
                            <div class="input-group input-group-lg mt-1">
                                <input type="password" class="form-control" id="password" name="password" required placeholder="••••••••" autocomplete="current-password">
                                <button class="btn btn-outline-secondary" type="button" id="togglePasswordBtn" aria-label="Toggle password visibility">
                                    <i class="bi bi-eye"></i>
                                </button>
                            </div>
                        </div>

                        <div class="d-grid gap-2 mt-4">
                            <button type="submit" class="btn btn-primary btn-lg fw-semibold" id="submitBtn">
                                <span>Sign In</span>
                            </button>
                        </div>
                    </form>

                    <div class="mt-4 pt-3 border-top text-center text-muted small">
                        Don't have an account yet? <a href="#/register" class="fw-semibold text-primary text-decoration-none">Create an account</a>
                    </div>

                    <div class="mt-4 p-3 rounded bg-body-tertiary border text-start small">
                        <div class="fw-bold mb-1"><i class="bi bi-key me-1"></i>Demo Accounts:</div>
                        <div><strong>Landlord:</strong> <code>landlord1@leaseflow.dev</code> / <code>Landlord@123</code></div>
                        <div><strong>Tenant:</strong> <code>tenant1@leaseflow.dev</code> / <code>Tenant@123</code></div>
                    </div>
                </div>
            </div>
        </div>
    `;
}

export function initLogin() {
    const form = document.getElementById('loginForm');
    const submitBtn = document.getElementById('submitBtn');
    const toggleBtn = document.getElementById('togglePasswordBtn');
    const passwordInput = document.getElementById('password');

    if (toggleBtn && passwordInput) {
        toggleBtn.onclick = () => {
            const isPassword = passwordInput.type === 'password';
            passwordInput.type = isPassword ? 'text' : 'password';
            toggleBtn.innerHTML = `<i class="bi bi-eye${isPassword ? '-slash' : ''}"></i>`;
        };
    }

    if (form) {
        form.onsubmit = async (e) => {
            e.preventDefault();
            clearFormErrors(form);

            const email = form.email.value.trim();
            const password = form.password.value;

            if (!email || !password) {
                toast.warning('Please enter both email and password.');
                return;
            }

            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span>Signing in...';

            try {
                const res = await api.post('/api/v1/auth/login', { email, password });
                auth.setSession(res);
                toast.success(`Welcome back, ${res.fullName}!`);

                if (res.role === 'ROLE_LANDLORD' || res.role === 'ROLE_ADMIN') {
                    window.location.hash = '#/landlord/dashboard';
                } else {
                    window.location.hash = '#/tenant/dashboard';
                }
            } catch (err) {
                if (err.errors) {
                    setFormErrors(form, err.errors);
                } else {
                    toast.error(err.message || 'Invalid email or password.');
                }
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<span>Sign In</span>';
            }
        };
    }
}

