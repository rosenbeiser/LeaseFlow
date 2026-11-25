/**
 * Client-Side Hash Router with Role Guards
 */

import { auth } from './auth.js';
import { toast } from './ui/toast.js';

class Router {
    constructor() {
        this.routes = [];
        this.currentCleanup = null;
        window.addEventListener('hashchange', () => this.handleRoute());
    }

    add(path, handler, options = {}) {
        // Support parameters like :id
        const paramNames = [];
        const regexPath = path.replace(/:([a-zA-Z0-9_]+)/g, (_, paramName) => {
            paramNames.push(paramName);
            return '([^/]+)';
        });

        this.routes.push({
            pattern: new RegExp(`^${regexPath}$`),
            paramNames,
            handler,
            roles: options.roles || [],
            title: options.title || 'LeaseFlow',
            layout: options.layout || 'app' // 'auth' or 'app'
        });
        return this;
    }

    navigate(hash) {
        window.location.hash = hash;
    }

    async handleRoute() {
        const hash = window.location.hash.slice(1) || '/login';
        const [path, queryString] = hash.split('?');
        const queryParams = new URLSearchParams(queryString || '');

        let match = null;
        let route = null;
        const params = {};

        for (const r of this.routes) {
            const m = path.match(r.pattern);
            if (m) {
                match = m;
                route = r;
                r.paramNames.forEach((name, index) => {
                    params[name] = m[index + 1];
                });
                break;
            }
        }

        if (!route) {
            this.renderNotFound();
            return;
        }

        // Role authorization guard
        if (route.roles.length > 0) {
            if (!auth.isAuthenticated()) {
                toast.warning('Please log in to continue.');
                this.navigate('#/login');
                return;
            }

            const currentRole = auth.getRole();
            if (!route.roles.includes(currentRole) && currentRole !== 'ROLE_ADMIN') {
                toast.error('Unauthorized access to this section.');
                if (auth.isLandlord()) {
                    this.navigate('#/landlord/dashboard');
                } else if (auth.isTenant()) {
                    this.navigate('#/tenant/dashboard');
                } else {
                    this.navigate('#/login');
                }
                return;
            }
        }

        document.title = `${route.title} — LeaseFlow`;

        if (typeof this.currentCleanup === 'function') {
            this.currentCleanup();
            this.currentCleanup = null;
        }

        try {
            this.currentCleanup = await route.handler(params, queryParams);
        } catch (err) {
            console.error('Route handler error:', err);
            toast.error('An error occurred while loading this view.');
        }
    }

    renderNotFound() {
        const appEl = document.getElementById('app');
        if (appEl) {
            appEl.innerHTML = `
                <div class="d-flex align-items-center justify-content-center min-vh-100 text-center p-4">
                    <div>
                        <h1 class="display-1 fw-bold text-primary">404</h1>
                        <h3 class="fw-semibold mb-2">Page Not Found</h3>
                        <p class="text-muted mb-4">The page you requested does not exist or has moved.</p>
                        <a href="#/login" class="btn btn-primary px-4 py-2">Back to Dashboard</a>
                    </div>
                </div>
            `;
        }
    }
}

export const router = new Router();

