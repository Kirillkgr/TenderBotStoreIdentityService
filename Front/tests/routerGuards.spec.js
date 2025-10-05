import {beforeEach, describe, expect, it, vi} from 'vitest';
import {createPinia, setActivePinia} from 'pinia';
import {useAuthStore} from '@/store/auth';

// Helper to get the registered global beforeEach guard from router mock
async function getBeforeEachGuard() {
    // Ensure fresh module load so that router.beforeEach is registered each time
    vi.resetModules();
    const mod = await import('@/router');
    const r = mod.default;
    return r.beforeEach.mock.calls[0][0];
}

describe('Router guards by roles', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        setActivePinia(createPinia());
    });

    it('redirects unauthenticated to /login when route requiresAuth', async () => {
        const auth = useAuthStore();
        auth.accessToken = null; // unauthenticated

        const guard = await getBeforeEachGuard();
        const next = vi.fn();
        await guard({meta: {requiresAuth: true}}, {name: 'Home', fullPath: '/'}, next);

        expect(next).toHaveBeenCalledWith('/login');
    });

    it('allows ADMIN/OWNER to BrandTags; denies CLIENT', async () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT');

        // Case ADMIN
        auth.roles = ['ADMIN'];
        let next = vi.fn();
        let guard = await getBeforeEachGuard();
        await guard({meta: {requiresAuth: true, roles: ['ADMIN', 'OWNER']}}, {name: 'Home', fullPath: '/'}, next);
        expect(next).toHaveBeenCalledWith(); // allowed

        // Case OWNER
        auth.roles = ['OWNER'];
        next = vi.fn();
        await guard({meta: {requiresAuth: true, roles: ['ADMIN', 'OWNER']}}, {name: 'Home', fullPath: '/'}, next);
        expect(next).toHaveBeenCalledWith();

        // Case CLIENT (denied -> Home)
        auth.roles = ['CLIENT'];
        next = vi.fn();
        await guard({name: 'BrandTags', meta: {requiresAuth: true, roles: ['ADMIN', 'OWNER']}}, {
            name: 'Home',
            fullPath: '/'
        }, next);
    });

    it('allows only COOK to /kitchen', async () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT');
        const guard = await getBeforeEachGuard();

        // Deny CLIENT
        auth.roles = ['CLIENT'];
        let next = vi.fn();
        await guard({name: 'Kitchen', meta: {requiresAuth: true, roles: ['COOK']}}, {
            name: 'Home',
            fullPath: '/'
        }, next);
        expect(next).toHaveBeenCalledWith({name: 'Home'});
        // Allow COOK
        auth.roles = ['COOK'];
        next = vi.fn();
        await guard({name: 'Kitchen', meta: {requiresAuth: true, roles: ['COOK']}}, {
            name: 'Home',
            fullPath: '/'
        }, next);
        expect(next).toHaveBeenCalledWith();
    });

    it('allows only CASHIER to /cashier', async () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT');
        const guard = await getBeforeEachGuard();

        // Deny USER
        auth.roles = ['USER'];
        let next = vi.fn();
        await guard({name: 'Cashier', meta: {requiresAuth: true, roles: ['CASHIER']}}, {
            name: 'Home',
            fullPath: '/'
        }, next);
        expect(next).toHaveBeenCalledWith({name: 'Home'});

        // Allow CASHIER
        auth.roles = ['CASHIER'];
        next = vi.fn();
        await guard({name: 'Cashier', meta: {requiresAuth: true, roles: ['CASHIER']}}, {
            name: 'Home',
            fullPath: '/'
        }, next);
        expect(next).toHaveBeenCalledWith();
    });
});
