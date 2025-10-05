// JWT utilities: decode and derive current role from claims

function base64UrlDecode(input) {
    try {
        const base64 = input.replace(/-/g, '+').replace(/_/g, '/');
        const padded = base64 + '==='.slice((base64.length + 3) % 4);
        if (typeof atob === 'function') {
            return atob(padded);
        }
        // Fallback for environments without atob (very rare in browser-only apps)
        return Buffer.from(padded, 'base64').toString('utf-8');
    } catch (_) {
        return '';
    }
}

export function decodeJwt(token) {
    try {
        if (!token || typeof token !== 'string') return {};
        const parts = token.split('.');
        if (parts.length < 2) return {};
        const payload = base64UrlDecode(parts[1]);
        const obj = JSON.parse(payload || '{}');
        return obj && typeof obj === 'object' ? obj : {};
    } catch (_) {
        return {};
    }
}

export function deriveCurrentRole(claims) {
    try {
        const roles = Array.isArray(claims?.roles) ? claims.roles : [];
        // If backend encodes membership role among roles, prioritize business roles
        const priority = ['OWNER', 'ADMIN', 'COOK', 'CASHIER', 'CLIENT', 'USER'];
        for (const r of priority) {
            if (roles.includes(r)) return r;
        }
        return 'USER';
    } catch (_) {
        return 'USER';
    }
}
