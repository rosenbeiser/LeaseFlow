/**
 * LeaseFlow API Client
 * Wraps Fetch API, manages JWT Authorization header, parses ApiResponse,
 * and handles 401 redirects and global error notifications.
 */

import { auth } from './auth.js';
import { toast } from './ui/toast.js';

export class ApiError extends Error {
    constructor(message, status = 500, errors = null) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.errors = errors;
    }
}

class ApiClient {
    constructor(baseUrl = '') {
        this.baseUrl = baseUrl;
    }

    async request(endpoint, options = {}) {
        const url = `${this.baseUrl}${endpoint}`;
        const headers = new Headers(options.headers || {});

        if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
            headers.set('Content-Type', 'application/json');
        }

        const token = auth.getToken();
        if (token) {
            headers.set('Authorization', `Bearer ${token}`);
        }

        const config = {
            ...options,
            headers
        };

        try {
            const response = await fetch(url, config);

            // Handle 401 Unauthorized
            if (response.status === 401) {
                auth.clearSession();
                const currentHash = window.location.hash;
                if (!currentHash.includes('/login') && !currentHash.includes('/register')) {
                    toast.error('Session expired. Please log in again.');
                    window.location.hash = '#/login';
                }
                throw new ApiError('Authentication required', 401);
            }

            // Parse JSON response
            let json = null;
            const text = await response.text();
            if (text) {
                try {
                    json = JSON.parse(text);
                } catch {
                    json = { message: text };
                }
            }

            if (!response.ok) {
                const message = (json && json.message) ? json.message : `HTTP Error ${response.status}`;
                const errors = (json && json.data && typeof json.data === 'object') ? json.data : null;

                if (response.status === 403) {
                    toast.warning(message || 'Access Denied: You do not have permission.');
                } else if (response.status >= 500) {
                    toast.error(message || 'Internal server error occurred.');
                }

                throw new ApiError(message, response.status, errors);
            }

            // Return unwrapped payload if standard ApiResponse
            if (json && typeof json === 'object' && 'success' in json && 'data' in json) {
                return json.data;
            }

            return json;
        } catch (error) {
            if (error instanceof ApiError) {
                throw error;
            }
            toast.error('Network communication error. Check your connection.');
            throw new ApiError(error.message || 'Network error', 0);
        }
    }

    get(endpoint, params = null) {
        let url = endpoint;
        if (params) {
            const query = new URLSearchParams();
            Object.entries(params).forEach(([k, v]) => {
                if (v !== undefined && v !== null && v !== '') {
                    query.append(k, v);
                }
            });
            const qs = query.toString();
            if (qs) url += (url.includes('?') ? '&' : '?') + qs;
        }
        return this.request(url, { method: 'GET' });
    }

    post(endpoint, body) {
        return this.request(endpoint, {
            method: 'POST',
            body: body instanceof FormData ? body : JSON.stringify(body)
        });
    }

    put(endpoint, body = null) {
        return this.request(endpoint, {
            method: 'PUT',
            body: body ? JSON.stringify(body) : undefined
        });
    }

    delete(endpoint) {
        return this.request(endpoint, { method: 'DELETE' });
    }
}

export const api = new ApiClient();
