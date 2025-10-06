import {describe, expect, it} from 'vitest';
import {render} from '@testing-library/vue';
import canDirective from '../src/directives/can';
import {createPinia, setActivePinia} from 'pinia';
import {useAuthStore} from '../src/store/auth';

function mountWithRoles(roles, binding) {
    setActivePinia(createPinia());
    const auth = useAuthStore();
    auth.roles = Array.isArray(roles) ? roles : [];
    const comp = {
        template: '<button data-testid="btn" title="X" v-can="binding">BTN</button>',
        setup() {
            return {binding};
        }
    };
    return render(comp, {global: {directives: {can: canDirective}}});
}

describe('ACL Smoke: v-can directive', () => {
    it('hide mode removes element from layout', async () => {
        const {getByTestId} = mountWithRoles([], {any: ['ADMIN'], mode: 'hide'});
        const btn = getByTestId('btn');
        expect(btn.style.display).toBe('none');
    });

    it('disable mode sets aria-disabled, pointer-events, opacity, and tooltip', async () => {
        const {getByTestId} = mountWithRoles([], {any: ['ADMIN'], mode: 'disable', tooltip: 'Недостаточно прав'});
        const btn = getByTestId('btn');
        expect(btn.getAttribute('aria-disabled')).toBe('true');
        expect(btn.style.pointerEvents).toBe('none');
        expect(btn.style.opacity).toBe('0.5');
        expect(btn.getAttribute('title')).toBe('Недостаточно прав');
    });

    it('no restriction returns visible/enabled', async () => {
        const {getByTestId} = mountWithRoles([], undefined);
        const btn = getByTestId('btn');
        expect(btn.style.display).not.toBe('none');
    });

    it('garbage binding does not crash and leaves element visible by default', async () => {
        const {getByTestId} = mountWithRoles([], 123);
        const btn = getByTestId('btn');
        expect(btn.style.display).not.toBe('none');
    });
});
