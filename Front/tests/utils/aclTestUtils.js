import {render} from '@testing-library/vue';
import {createPinia, setActivePinia} from 'pinia';
import canDirective from '../../src/directives/can';
import {useAuthStore} from '../../src/store/auth';

export function setupPiniaAuth({roles = [], memberships = [], extras = {}} = {}) {
    setActivePinia(createPinia());
    const auth = useAuthStore();
    if (auth.setAccessToken) auth.setAccessToken('AT');
    auth.roles = Array.isArray(roles) ? roles : [];
    auth.memberships = Array.isArray(memberships) ? memberships : [];
    if (extras && typeof extras === 'object') Object.assign(auth, extras);
    if (!auth.selectMembership) {
        auth.selectMembership = async (m) => {
            // emulate switching context: patch membershipId/brandId and add role to roles if missing
            try {
                auth.membershipId = m.membershipId ?? m.id;
            } catch (_) {
            }
            try {
                auth.brandId = m.brandId;
            } catch (_) {
            }
            const r = (m.role || m.membershipRole || m.brandRole || m.locationRole || '').toUpperCase().replace(/^ROLE_/, '');
            if (r && !auth.roles.includes(r)) {
                auth.roles = [...auth.roles, r];
                auth.$patch({roles: auth.roles});
            }
        };
    }
    return auth;
}

export function renderWithAcl(component, {props = {}, useRealDirective = true} = {}) {
    const globals = {
        stubs: {'router-link': {template: '<a><slot /></a>'}},
    };
    if (useRealDirective) {
        globals.directives = {can: canDirective};
    } else {
        globals.directives = {
            can: {
                mounted() {
                }, updated() {
                }
            }
        };
    }
    return render(component, {props, global: globals});
}

// Router mocks for click navigation assertions
export const pushMock = vi.fn(() => Promise.resolve());
export const replaceMock = vi.fn(() => Promise.resolve());

export function mockVueRouterForPush() {
    vi.mock('vue-router', async (orig) => {
        const actual = await orig();
        return {
            ...actual,
            useRouter: () => ({push: pushMock, replace: replaceMock, currentRoute: {value: {fullPath: '/'}}}),
            useRoute: () => ({name: 'Home'})
        };
    });
}
