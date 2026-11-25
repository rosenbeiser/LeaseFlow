/**
 * Register Page Module
 */

import { api } from '../api.js';
import { auth } from '../auth.js';
import { toast } from '../ui/toast.js';
import { clearFormErrors, setFormErrors } from '../ui/form.js';

export function renderRegister() {
    return `
        <div class="row min-vh-100 g-0">
            <!-- Left Branding Column -->
            <div class="col-lg-6 d-none d-lg-flex flex-column justify-content-between p-5 text-white" style="background: linear-gradient(135deg, #0f766e 0%, #134e4a 100%);">
                <div>
                    <div class="d-flex align-items-center gap-2 mb-4">
                        <i class="bi bi-building-gear fs-3 text-warning"></i>
                        <span class="fs-4 fw-bold letter-spacing-tight">LeaseFlow</span>
                    </div>
                    <div class="mt-5">
                        <span class="badge bg-white bg-opacity-10 text-white border border-white border-opacity-25 px-3 py-2 rounded-pill mb-3">Get Started Today</span>
                        <h1 class="display-5 fw-bold leading-tight">Create your LeaseFlow Account</h1>
                        <p class="lead opacity-75 mt-3">
                            Join property owners and tenants streamlining leases, metered utility rates, and real-time payment reconciliation.
                        </p>
                    </div>
                </div>
                <div class="small opacity-50">
                    &copy; ${new Date().getFullYear()} LeaseFlow. Transparent billing & automated lease states.
                </div>
            </div>

            <!-- Right Form Column -->
            <div class="col-lg-6 d-flex align-items-center justify-content-center p-4 p-md-5">
                <div class="w-100" style="max-width: 460px;">
                    <h2 class="fw-bold mb-1">Create an account</h2>
                    <p class="text-muted mb-4">Select your role and enter your details to get started</p>

                    <form id="registerForm" novalidate>
                        <div class="mb-3">
                            <label class="form-label fw-semibold">I am registering as a</label>
                            <div class="row g-2">
                                <div class="col-6">
                                    <input type="radio" class="btn-check" name="role" id="roleLandlord" value="ROLE_LANDLORD" checked>
                                    <label class="btn btn-outline-primary w-100 py-2 d-flex flex-column align-items-center gap-1" for="roleLandlord">
                                        <i class="bi bi-building fs-5"></i>
                                        <span>Landlord</span>
                                    </label>
                                </div>
                                <div class="col-6">
                                    <input type="radio" class="btn-check" name="role" id="roleTenant" value="ROLE_TENANT">
                                    <label class="btn btn-outline-primary w-100 py-2 d-flex flex-column align-items-center gap-1" for="roleTenant">
                                        <i class="bi bi-person-badge fs-5"></i>
                                        <span>Tenant</span>
                                    </label>
                                </div>
                            </div>
                        </div>

                        <div class="mb-3">
                            <label for="fullName" class="form-label fw-semibold">Full name</label>
                            <input type="text" class="form-control" id="fullName" name="fullName" required placeholder="Alice Johnson" autocomplete="name">
                        </div>

                        <div class="mb-3">
                            <label for="email" class="form-label fw-semibold">Email address</label>
                            <input type="email" class="form-control" id="email" name="email" required placeholder="alice@example.com" autocomplete="email">
                        </div>

                        <div class="mb-3">
                            <label for="phone" class="form-label fw-semibold">Phone number (optional)</label>
                            <input type="tel" class="form-control" id="phone" name="phone" placeholder="+1 (555) 000-0000" autocomplete="tel">
                        </div>

                        <div class="mb-3">
                            <label for="password" class="form-label fw-semibold">Password</label>
                            <div class="input-group">
                                <input type="password" class="form-control" id="password" name="password" required placeholder="Min 8 characters" autocomplete="new-password">
                                <button class="btn btn-outline-secondary" type="button" id="togglePasswordBtn" aria-label="Toggle password visibility">
                                    <i class="bi bi-eye"></i>
                                </button>
                            </div>
                            <div class="progress mt-2" style="height: 4px;">
                                <div id="passwordStrengthBar" class="progress-bar bg-danger" role="progressbar" style="width: 0%"></div>
                            </div>
                            <div class="small text-muted mt-1" id="passwordStrengthText">Enter at least 8 characters</div>
                        </div>

                        <div class="d-grid gap-2 mt-4">
                            <button type="submit" class="btn btn-primary btn-lg fw-semibold" id="submitBtn">
                                <span>Create Account</span>
                            </button>
                        </div>
                    </form>

                    <div class="mt-4 pt-3 border-top text-center text-muted small">
                        Already have an account? <a href="#/login" class="fw-semibold text-primary text-decoration-none">Sign in</a>
                    </div>
                </div>
            </div>
        </div>
    `;
}

export function initRegister() {
    const form = document.getElementById('registerForm');
    const submitBtn = document.getElementById('submitBtn');
    const toggleBtn = document.getElementById('togglePasswordBtn');
    const passwordInput = document.getElementById('password');
    const strengthBar = document.getElementById('passwordStrengthBar');
    const strengthText = document.getElementById('passwordStrengthText');

    if (toggleBtn && passwordInput) {
        toggleBtn.onclick = () => {
            const isPassword = passwordInput.type === 'password';
            passwordInput.type = isPassword ? 'text' : 'password';
            toggleBtn.innerHTML = `<i class="bi bi-eye${isPassword ? '-slash' : ''}"></i>`;
        };
    }

    if (passwordInput && strengthBar && strengthText) {
        passwordInput.oninput = () => {
            const val = passwordInput.value;
            let score = 0;
            if (val.length >= 8) score++;
            if (/[A-Z]/.test(val)) score++;
            if (/[0-9]/.test(val)) score++;
            if (/[^A-Za-z0-9]/.test(val)) score++;

            const widths = ['0%', '25%', '50%', '75%', '100%'];
            const colors = ['bg-danger', 'bg-danger', 'bg-warning', 'bg-info', 'bg-success'];
            const labels = ['Too short', 'Weak', 'Fair', 'Good', 'Strong'];

            strengthBar.style.width = widths[score];
            strengthBar.className = `progress-bar ${colors[score]}`;
            strengthText.textContent = val.length === 0 ? 'Enter at least 8 characters' : `Strength: ${labels[score]}`;
        };
    }

    if (form) {
        form.onsubmit = async (e) => {
            e.preventDefault();
            clearFormErrors(form);

            const role = form.querySelector('input[name="role"]:checked')?.value;
            const fullName = form.fullName.value.trim();
            const email = form.email.value.trim();
            const phone = form.phone.value.trim();
            const password = form.password.value;

            if (!fullName || !email || !password) {
                toast.warning('Please fill in all required fields.');
                return;
            }

            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span>Creating account...';

            try {
                const res = await api.post('/api/v1/auth/register', {
                    fullName,
                    email,
                    phone: phone || null,
                    password,
                    role
                });

                auth.setSession(res);
                toast.success('Registration successful! Welcome to LeaseFlow.');

                if (res.role === 'ROLE_LANDLORD') {
                    window.location.hash = '#/landlord/dashboard';
                } else {
                    window.location.hash = '#/tenant/dashboard';
                }
            } catch (err) {
                if (err.errors) {
                    setFormErrors(form, err.errors);
                } else {
                    toast.error(err.message || 'Registration failed.');
                }
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<span>Create Account</span>';
            }
        };
    }
}

