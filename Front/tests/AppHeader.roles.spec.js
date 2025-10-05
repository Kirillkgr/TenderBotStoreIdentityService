import {beforeEach, describe, expect, it} from 'vitest';
import {render, screen} from '@testing-library/vue';
import {createPinia, setActivePinia} from 'pinia';
import AppHeader from '@/components/AppHeader.vue';
import {useAuthStore} from '@/store/auth';

describe('AppHeader role-based menu visibility', () => {
    beforeEach(() => {
        setActivePinia(createPinia());
    });

    function renderHeader() {
        return render(AppHeader, {
            props: {isModalVisible: false},
            global: {stubs: {'router-link': {template: '<a><slot /></a>'}}}
        });
    }

    it('shows Kitchen link only for COOK', async () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT');
        auth.roles = ['CLIENT'];
        let utils = renderHeader();
        expect(screen.queryByText('Кухня')).toBeNull();

        // Switch role to COOK
        auth.roles = ['COOK'];
        utils.unmount();
        utils = renderHeader();
        expect(screen.getAllByText('Кухня')[0]).toBeTruthy();
    });

    it('shows Cashier link only for CASHIER', async () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT');
        auth.roles = ['USER'];
        let utils = renderHeader();
        expect(screen.queryByText('Касса')).toBeNull();

        auth.roles = ['CASHIER'];
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
