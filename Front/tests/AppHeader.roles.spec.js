import {beforeEach, describe, expect, it} from 'vitest';
import {render, screen} from '@testing-library/vue';
import {createPinia, setActivePinia} from 'pinia';
import AppHeader from '@/components/AppHeader.vue';
import {useAuthStore} from '@/store/auth';

describe('AppHeader role-based menu visibility', () => {
    beforeEach(() => {
        setActivePinia(createPinia());
    });

    it('shows Orders link for CASHIER and ADMIN/OWNER, but not for CLIENT', () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT');

        auth.roles = ['CLIENT'];
        let utils = renderHeader();
        expect(screen.queryByText('Заказы')).toBeNull();

        auth.roles = ['CASHIER'];
        utils.unmount();
        utils = renderHeader();
        expect(screen.getAllByText('Заказы')[0]).toBeTruthy();

        auth.roles = ['ADMIN'];
        utils.unmount();
        utils = renderHeader();
        expect(screen.getAllByText('Заказы')[0]).toBeTruthy();

        auth.roles = ['OWNER'];
        utils.unmount();
        utils = renderHeader();
        expect(screen.getAllByText('Заказы')[0]).toBeTruthy();
    });

    function renderHeader() {
        return render(AppHeader, {
            props: {isModalVisible: false},
            global: {
                stubs: {'router-link': {template: '<a><slot /></a>'}},
                directives: {
                    can: {
                        mounted() {
                        }, updated() {
                        }
                    }
                }
            }
        });
    }

    it('shows Kitchen link for COOK and ADMIN/OWNER', async () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT');
        auth.roles = ['CLIENT'];
        let utils = renderHeader();
        expect(screen.queryByText('Кухня')).toBeNull();

        // COOK
        auth.roles = ['COOK'];
        utils.unmount();
        utils = renderHeader();
        expect(screen.getAllByText('Кухня')[0]).toBeTruthy();

        // ADMIN
        auth.roles = ['ADMIN'];
        utils.unmount();
        utils = renderHeader();
        expect(screen.getAllByText('Кухня')[0]).toBeTruthy();

        // OWNER
        auth.roles = ['OWNER'];
        utils.unmount();
        utils = renderHeader();
        expect(screen.getAllByText('Кухня')[0]).toBeTruthy();
    });

    it('shows Cashier link for CASHIER and ADMIN/OWNER', async () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT');
        auth.roles = ['USER'];
        let utils = renderHeader();
        expect(screen.queryByText('Касса')).toBeNull();

        // CASHIER
        auth.roles = ['CASHIER'];
        utils.unmount();
        utils = renderHeader();
        expect(screen.getAllByText('Касса')[0]).toBeTruthy();

        // ADMIN
        auth.roles = ['ADMIN'];
        utils.unmount();
        utils = renderHeader();
        expect(screen.getAllByText('Касса')[0]).toBeTruthy();

        // OWNER
        auth.roles = ['OWNER'];
        utils.unmount();
        utils = renderHeader();
        expect(screen.getAllByText('Касса')[0]).toBeTruthy();
    });

    it('shows Admin link only for ADMIN/OWNER', () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT');

        auth.roles = ['CLIENT'];
        let utils = renderHeader();
        expect(screen.queryByText('Админ')).toBeNull();

        auth.roles = ['ADMIN'];
        utils.unmount();
        utils = renderHeader();
        expect(screen.getAllByText('Админ')[0]).toBeTruthy();

        auth.roles = ['OWNER'];
        utils.unmount();
        utils = renderHeader();
        expect(screen.getAllByText('Админ')[0]).toBeTruthy();
    });
});
