import {beforeEach, describe, expect, it} from 'vitest';
import {createPinia, setActivePinia} from 'pinia';
import {useAcl} from '@/composables/useAcl';
import {useAuthStore} from '@/store/auth';

describe('useAcl composable', () => {
    beforeEach(() => {
        setActivePinia(createPinia());
    });

    it('returns false when no roles', () => {
        const {can, any, all} = useAcl();
        expect(any(['ADMIN'])).toBe(false);
        expect(all(['ADMIN'])).toBe(false);
        expect(can(['ADMIN'])).toBe(false);
    });

    it('supports any() and all() checks', () => {
        const auth = useAuthStore();
        auth.roles = ['CLIENT', 'COOK'];
        const {any, all, can} = useAcl();

        expect(any(['ADMIN', 'COOK'])).toBe(true);
        expect(all(['CLIENT', 'COOK'])).toBe(true);
        expect(all(['CLIENT', 'ADMIN'])).toBe(false);

        expect(can(['ADMIN', 'OWNER'])).toBe(false);
        expect(can(['COOK', 'OWNER'])).toBe(true);
        expect(can({all: ['CLIENT', 'COOK']})).toBe(true);
        expect(can({any: ['ADMIN', 'OWNER']})).toBe(false);
    });
});
