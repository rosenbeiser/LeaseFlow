/**
 * LeaseFlow Authentication Manager
 * Manages user credentials in sessionStorage, decodes JWT claims,
 * and enforces client-side route authorization guards.
 */

const TOKEN_KEY = 'lf_access_token';
const USER_KEY  = 'lf_user_info';

class AuthManager {
    getToken() {
        return sessionStorage.getItem(TOKEN_KEY);
    }

    getUser() {
        const raw = sessionStorage.getItem(USER_KEY);
        try {
            return raw ? JSON.parse(raw) : null;
        } catch {
            return null;
        }
    }

    setSession(authData) {
        if (!authData || !authData.accessToken) return;
        sessionStorage.setItem(TOKEN_KEY, authData.accessToken);

        const userInfo = {
            userId: authData.userId,
            email: authData.email,
            fullName: authData.fullName,
            role: authData.role
        };
        sessionStorage.setItem(USER_KEY, JSON.stringify(userInfo));
    }

    clearSession() {
        sessionStorage.removeItem(TOKEN_KEY);
        sessionStorage.removeItem(USER_KEY);
    }

    isAuthenticated() {
        const token = this.getToken();
        if (!token) return false;
        try {
            const parts = token.split('.');
            if (parts.length !== 3) return false;
            const payload = JSON.parse(atob(parts[1]));
            return payload.exp * 1000 > Date.now();
        } catch {
            return false;
        }
    }

    getRole() {
        const user = this.getUser();
        if (user && user.role) return user.role;
        const token = this.getToken();
        if (!token) return null;
        try {
            const parts = token.split('.');
            const payload = JSON.parse(atob(parts[1]));
            return payload.role || null;
        } catch {
            return null;
        }
    }

    isLandlord() {
        return this.getRole() === 'ROLE_LANDLORD';
    }

    isTenant() {
        return this.getRole() === 'ROLE_TENANT';
    }

    isAdmin() {
        return this.getRole() === 'ROLE_ADMIN';
    }

    logout() {
        this.clearSession();
        window.location.hash = '#/login';
        window.location.reload();
    }
}

export const auth = new AuthManager();

