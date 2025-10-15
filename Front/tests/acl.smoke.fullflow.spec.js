import {beforeEach, describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/vue';
import {createPinia, setActivePinia} from 'pinia';
import Sidebar from '@/components/Sidebar.vue';
import {renderWithAcl} from './utils/aclTestUtils';
import {useUiStore} from '@/store/ui';
import AdminOrdersView from '@/views/AdminOrdersView.vue';
import {useAuthStore} from '../src/store/auth';
import canDirective from '../src/directives/can';
// Services mocks for orders
import accessible from './fixtures/orders/accessible.json';

// Router mocks
const pushMock = vi.fn(() => Promise.resolve());
vi.mock('vue-router', async (orig) => {
    const actual = await orig();
    return {
        ...actual,
        useRouter: () => ({push: pushMock, replace: vi.fn(), currentRoute: {value: {fullPath: '/'}}}),
        useRoute: () => ({name: 'Home'})
    };
});

vi.mock('@/services/notifications', () => ({
    getNotificationsClient: () => ({
        start: () => {
        }, subscribe: () => () => {
        }
    })
}));
vi.mock('@/services/orderAdminService', () => ({
    default: {
        listAccessibleOrders: async () => accessible,
        listBrandOrders: async () => accessible,
        updateStatus: async () => ({})
    },
    ORDER_STATUSES: ['QUEUED', 'PREPARING', 'READY_FOR_PICKUP', 'OUT_FOR_DELIVERY', 'DELIVERED', 'COMPLETED', 'CANCELED']
}));

function setupAuth({roles = [], memberships = [], selectMembershipImpl} = {}) {
    setActivePinia(createPinia());
    const auth = useAuthStore();
    auth.setAccessToken?.('AT');
    auth.roles = roles;
    auth.memberships = memberships;
    if (selectMembershipImpl) auth.selectMembership = selectMembershipImpl;
    return auth;
}

describe('ACL Smoke: Full flow (context -> Orders -> change status)', () => {
    beforeEach(() => {
        vi.resetModules();
        pushMock.mockClear();
    });

    it('switches context via AppHeader and changes status in AdminOrders', async () => {
        // Step 1: ensure Orders is available in Sidebar for CASHIER role
        setupAuth({
            roles: [],
            memberships: [{id: 9, role: 'CASHIER', brandId: 1}],
            selectMembershipImpl: async function (m) {
                this.membershipId = m.id;
                this.brandId = m.brandId ?? null;
                if (!this.roles.includes('CASHIER')) {
                    this.roles = [...this.roles, 'CASHIER'];
                    this.$patch({roles: this.roles});
                }
            }
        });
        // Применяем membership, чтобы роль CASHIER попала в auth.roles
        const auth = useAuthStore();
        await auth.selectMembership({id: 9, role: 'CASHIER', brandId: 1});
        const ui = useUiStore();
        ui.isDesktop = true;
        ui.sidebarOpen = true;
        ui.sidebarCollapsed = false;
        renderWithAcl(Sidebar, {});
        const marketingBtn = Array.from(document.querySelectorAll('button.group-btn')).find(b => b.textContent?.includes('Маркетинг'));
        if (marketingBtn) await fireEvent.click(marketingBtn);
        // подождём появления ссылки
        let ordersLink = null;
        const start = Date.now();
        while (!ordersLink && Date.now() - start < 400) {
            ordersLink = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes('Заказы'));
            if (!ordersLink) await new Promise(r => setTimeout(r, 10));
        }
        expect(ordersLink && getComputedStyle(ordersLink).display !== 'none').toBe(true);

        // Step 2: render orders and change status
        render(AdminOrdersView, {global: {directives: {can: canDirective}}});
        await new Promise(r => setTimeout(r, 0));
        const allSelects = Array.from(document.querySelectorAll('select'));
        const statusSelect = allSelects.find(s => Array.from(s.options || []).some(o => ['QUEUED', 'PREPARING', 'DELIVERED', 'CANCELED'].includes(o.value)));
        await fireEvent.update(statusSelect, 'PREPARING');
        const save = await screen.findByText('Сохранить');
        expect(save.getAttribute('aria-disabled')).not.toBe('true');
        expect(save.disabled).toBe(false);
    });
});
