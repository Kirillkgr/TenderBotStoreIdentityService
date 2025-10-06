import {beforeEach, describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/vue';
import {createPinia, setActivePinia} from 'pinia';
import AdminOrdersView from '@/views/AdminOrdersView.vue';
import canDirective from '../src/directives/can';
import {useAuthStore} from '../src/store/auth';

// Fixtures
import accessible from './fixtures/orders/accessible.json';

// Mocks
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

function renderWithRoles(roles) {
    setActivePinia(createPinia());
    const auth = useAuthStore();
    auth.setAccessToken?.('AT');
    auth.roles = roles;
    return render(AdminOrdersView, {global: {directives: {can: canDirective}}});
}

describe('ACL Smoke: AdminOrdersView', () => {
    beforeEach(() => vi.resetModules());

    it('ADMIN can change status (Save enabled after change)', async () => {
        renderWithRoles(['ADMIN']);
        const selects = await screen.findAllByRole('combobox');
        const select = selects.find(s => Array.from(s.options || []).some(o => ['QUEUED', 'PREPARING', 'DELIVERED', 'CANCELED'].includes(o.value)));
        await fireEvent.update(select, 'DELIVERED');
        const save = await screen.findByText('Сохранить');
        expect(save.getAttribute('aria-disabled')).not.toBe('true');
        expect(save.disabled).toBe(false);
    });

    it('USER cannot change status (Save disabled by v-can)', async () => {
        renderWithRoles(['USER']);
        const selects = await screen.findAllByRole('combobox');
        const select = selects.find(s => Array.from(s.options || []).some(o => ['QUEUED', 'PREPARING', 'DELIVERED', 'CANCELED'].includes(o.value)));
        await fireEvent.update(select, 'DELIVERED');
        const save = await screen.findByText('Сохранить');
        expect(save.getAttribute('aria-disabled')).toBe('true');
    });
});
