import {beforeEach, describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/vue';
import {nextTick} from 'vue';
import {createPinia, setActivePinia} from 'pinia';
import AdminOrdersView from '@/views/AdminOrdersView.vue';
import {useAuthStore} from '@/store/auth';

// Mock notifications client to prevent network and side effects
vi.mock('@/services/notifications', () => ({
    getNotificationsClient: () => ({
        start: () => {
        }, subscribe: () => () => {
        }
    })
}));

// Mock orderAdminService to return one order so that Save button is present
vi.mock('@/services/orderAdminService', () => ({
    default: {
        listAccessibleOrders: async () => ({
            data: {
                content: [{id: 1, status: 'QUEUED', total: 100, items: []}],
                totalPages: 1,
                number: 0,
                size: 10
            }
        }),
        listBrandOrders: async () => ({
            data: {
                content: [{id: 1, status: 'QUEUED', total: 100, items: []}],
                totalPages: 1,
                number: 0,
                size: 10
            }
        }),
        updateStatus: async () => ({})
    },
    ORDER_STATUSES: ['QUEUED', 'PREPARING', 'READY_FOR_PICKUP', 'OUT_FOR_DELIVERY', 'DELIVERED', 'COMPLETED', 'CANCELED']
}));

const globalStubs = {
    directives: {
        can: {
            mounted() {
            }, updated() {
            }
        }
    }
};

describe('AdminOrdersView role permissions', () => {
    beforeEach(() => {
        setActivePinia(createPinia());
    });

    it('enables Save button for CASHIER', async () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT');
        auth.roles = ['CASHIER'];

        render(AdminOrdersView, {global: globalStubs});

        // Ждём появления строки заказа
        const saveButtons = await screen.findAllByText('Сохранить');
        expect(saveButtons.length).toBeGreaterThan(0);
        const btn = saveButtons[0];

        // Изначально драфт равен текущему статусу (кнопка заблокирована логикой "без изменений").
        // Меняем статус в select на другой, чтобы кнопка стала активной.
        const selects = screen.getAllByRole('combobox');
        expect(selects.length).toBeGreaterThan(0);
        const select = selects[0];
        // выберем статус PREPARING (он есть в статусах для QUEUED)
        await fireEvent.change(select, {target: {value: 'PREPARING'}});
        await nextTick();

        // Теперь кнопка должна быть активной (если v-can не блокирует для CASHIER)
        expect(btn.getAttribute('aria-disabled')).not.toBe('true');
        expect(btn.disabled).toBe(false);
    });
});
