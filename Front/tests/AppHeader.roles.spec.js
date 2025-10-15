import {beforeEach, describe, expect, it} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/vue';
import {createPinia, setActivePinia} from 'pinia';
import Sidebar from '@/components/Sidebar.vue';
import {useAuthStore} from '@/store/auth';
import {useUiStore} from '@/store/ui';

describe('Sidebar role-based menu visibility', () => {
    beforeEach(() => {
        setActivePinia(createPinia());
    });

    it('shows Orders link for CASHIER and ADMIN/OWNER, but not for CLIENT', async () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT');
        const ui = useUiStore();
        ui.isDesktop = true;
        ui.sidebarOpen = true;
        ui.sidebarCollapsed = false;
        auth.roles = ['CLIENT'];
        let utils = renderSidebar();
        await expandMarketing();
        expect(screen.queryByText('Заказы')).toBeNull();

        auth.roles = ['CASHIER'];
        utils.unmount();
        utils = renderSidebar();
        await expandMarketing();
        expect(screen.getAllByText('Заказы')[0]).toBeTruthy();

        auth.roles = ['ADMIN'];
        utils.unmount();
        utils = renderSidebar();
        await expandMarketing();
        expect(screen.getAllByText('Заказы')[0]).toBeTruthy();

        auth.roles = ['OWNER'];
        utils.unmount();
        utils = renderSidebar();
        await expandMarketing();
        expect(screen.getAllByText('Заказы')[0]).toBeTruthy();
    });

    function renderSidebar() {
        return render(Sidebar, {
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

    async function expandMarketing() {
        const btn = Array.from(document.querySelectorAll('button.group-btn')).find(b => b.textContent?.includes('Маркетинг'));
        if (btn) await fireEvent.click(btn);
    }

    it('shows Kitchen link for COOK and ADMIN/OWNER', async () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT');
        const ui = useUiStore();
        ui.isDesktop = true;
        ui.sidebarOpen = true;
        ui.sidebarCollapsed = false;
        auth.roles = ['CLIENT'];
        let utils = renderSidebar();
        await expandOps();
        expect(screen.queryByText('Кухня')).toBeNull();

        // COOK
        auth.roles = ['COOK'];
        utils.unmount();
        utils = renderSidebar();
        await expandOps();
        expect(screen.getAllByText('Кухня')[0]).toBeTruthy();

        // ADMIN
        auth.roles = ['ADMIN'];
        utils.unmount();
        utils = renderSidebar();
        await expandOps();
        expect(screen.getAllByText('Кухня')[0]).toBeTruthy();

        // OWNER
        auth.roles = ['OWNER'];
        utils.unmount();
        utils = renderSidebar();
        await expandOps();
        expect(screen.getAllByText('Кухня')[0]).toBeTruthy();
    });

    it('shows Cashier link for CASHIER and ADMIN/OWNER', async () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT');
        auth.roles = ['USER'];
        const ui = useUiStore();
        ui.isDesktop = true;
        ui.sidebarOpen = true;
        ui.sidebarCollapsed = false;
        let utils = renderSidebar();
        await expandOps();
        expect(screen.queryByText('Касса')).toBeNull();

        // CASHIER
        auth.roles = ['CASHIER'];
        utils.unmount();
        utils = renderSidebar();
        await expandOps();
        expect(screen.getAllByText('Касса')[0]).toBeTruthy();

        // ADMIN
        auth.roles = ['ADMIN'];
        utils.unmount();
        utils = renderHeader();
        expect(screen.getAllByText('Касса')[0]).toBeTruthy();

        // OWNER
        auth.roles = ['OWNER'];
        utils.unmount();
        utils = renderSidebar();
        await expandOps();
        expect(screen.getAllByText('Касса')[0]).toBeTruthy();
    });

    it('shows Admin link only for ADMIN/OWNER', () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT');

        auth.roles = ['CLIENT'];
        const ui = useUiStore();
        ui.isDesktop = true;
        ui.sidebarOpen = true;
        ui.sidebarCollapsed = false;
        let utils = renderSidebar();
        expect(screen.queryByText('Админ')).toBeNull();

        auth.roles = ['ADMIN'];
        utils.unmount();
        utils = renderSidebar();
        expect(screen.getAllByText('Админ')[0]).toBeTruthy();

        auth.roles = ['OWNER'];
        utils.unmount();
        utils = renderSidebar();
        expect(screen.getAllByText('Админ')[0]).toBeTruthy();
    });
});
