import {beforeEach, describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/vue';
import {createPinia, setActivePinia} from 'pinia';
import AppHeader from '@/components/AppHeader.vue';
import {useAuthStore} from '@/store/auth';

// Mock vue-router so that useRouter().push is captured
const pushMock = vi.fn(() => Promise.resolve());
vi.mock('vue-router', async (orig) => {
    const actual = await orig();
    return {
        ...actual,
        useRouter: () => ({push: pushMock, replace: vi.fn(), currentRoute: {value: {fullPath: '/'}}}),
        useRoute: () => ({name: 'Home'})
    };
});

function renderHeader() {
    const canDirective = {
        mounted() {
        }, updated() {
        }
    };
    return render(AppHeader, {
        props: {isModalVisible: false},
        global: {
            directives: {can: canDirective},
            stubs: {'router-link': {template: '<a><slot /></a>'}}
        }
    });
}

describe('AppHeader membership auto-switch navigation', () => {
    beforeEach(() => {
        setActivePinia(createPinia());
    });

    it('click Orders triggers context switch, waits roles, and navigates', async () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT');
        // start with no roles but with a CASHIER membership available
        auth.roles = [];
        auth.memberships = [{id: 9, role: 'CASHIER', brandId: 1, locationId: 1}];

        // spy on selectMembership to simulate token switch and roles update via $subscribe
        const selectSpy = vi.spyOn(auth, 'selectMembership').mockImplementation(async () => {
            // simulate async switch without network
            setTimeout(() => {
                auth.roles = ['CASHIER'];
                auth.$patch({roles: auth.roles});
            }, 10);
        });

        renderHeader();

        // Click Orders link
        const ordersLink = await screen.findAllByText('Заказы');
        await fireEvent.click(ordersLink[0]);

        // Wait until router.push called with /admin/orders
        await new Promise(r => setTimeout(r, 100));
        expect(selectSpy).toHaveBeenCalled();
        expect(pushMock).toHaveBeenCalled();
        expect(pushMock.mock.calls.some(args => args[0] === '/admin/orders' || (args[0]?.name === 'AdminOrders'))).toBe(true);
    });
});
