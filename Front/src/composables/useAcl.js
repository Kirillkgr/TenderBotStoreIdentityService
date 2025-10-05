import {computed} from 'vue';
import {useAuthStore} from '@/store/auth';

// Simple ACL helper
// Usage:
// const { can, any, all, roles } = useAcl();
// can(['ADMIN','OWNER'])
// can({ any: ['COOK','CASHIER'] })
export function useAcl() {
    const auth = useAuthStore();
    const roles = computed(() => Array.isArray(auth.roles) ? auth.roles : []);

    function hasAny(target = []) {
        const r = roles.value;
        if (!r || r.length === 0) return false;
        return target.some(t => r.includes(t));
    }

    function hasAll(target = []) {
        const r = roles.value;
        if (!r || r.length === 0) return false;
        return target.every(t => r.includes(t));
    }

    // can(input):
    // - Array => ANY of roles
    // - Object => { any?: string[], all?: string[] }
    function can(input) {
        if (!input) return true; // no restriction
        if (Array.isArray(input)) return hasAny(input);
        if (typeof input === 'object') {
            if (input.all && Array.isArray(input.all) && input.all.length > 0) return hasAll(input.all);
            if (input.any && Array.isArray(input.any) && input.any.length > 0) return hasAny(input.any);
            return true;
        }
        return true;
    }

    return {roles, any: hasAny, all: hasAll, can};
}
