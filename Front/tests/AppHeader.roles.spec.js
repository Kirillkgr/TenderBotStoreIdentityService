import {beforeEach, describe, expect, it} from 'vitest';
import {fireEvent} from '@testing-library/vue';
import {createPinia, setActivePinia} from 'pinia';
import Sidebar from '@/components/Sidebar.vue';
import {useAuthStore} from '@/store/auth';
import {useUiStore} from '@/store/ui';
import {renderWithAcl} from './utils/aclTestUtils';

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
        const hiddenOrdersEl = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes('Заказы'));
        const hiddenOrders = !hiddenOrdersEl || getComputedStyle(hiddenOrdersEl).display === 'none';
        expect(hiddenOrders).toBe(true);

        auth.roles = ['CASHIER'];
        auth.brandId = 1;
        auth.membershipId = 1;
        utils.unmount();
        try {
            localStorage.setItem('ui_sidebar_expanded', JSON.stringify({sklad: true, marketing: true, ops: true}));
        } catch (_) {
        }
        utils = renderSidebar();
        await new Promise(r => setTimeout(r, 10));
        await expandMarketing();
        await new Promise(r => setTimeout(r, 20));
        const visibleOrders = await waitForMenuLink('Заказы');
        expect(visibleOrders && getComputedStyle(visibleOrders).display !== 'none').toBe(true);

        auth.roles = ['ADMIN'];
        auth.brandId = 1;
        auth.membershipId = 1;
        utils.unmount();
        try {
            localStorage.setItem('ui_sidebar_expanded', JSON.stringify({sklad: true, marketing: true, ops: true}));
        } catch (_) {
        }
        utils = renderSidebar();
        await new Promise(r => setTimeout(r, 10));
        await expandMarketing();
        await new Promise(r => setTimeout(r, 20));
        const adminOrders = await waitForMenuLink('Заказы');
        expect(adminOrders).toBeTruthy();

        auth.roles = ['OWNER'];
        auth.brandId = 1;
        auth.membershipId = 1;
        utils.unmount();
        try {
            localStorage.setItem('ui_sidebar_expanded', JSON.stringify({sklad: true, marketing: true, ops: true}));
        } catch (_) {
        }
        utils = renderSidebar();
        await new Promise(r => setTimeout(r, 10));
        await expandMarketing();
        await new Promise(r => setTimeout(r, 20));
        const ownerOrders = await waitForMenuLink('Заказы');
        expect(ownerOrders).toBeTruthy();
    });

    function renderSidebar() {
        return renderWithAcl(Sidebar, {});
    }

    async function waitForMenuLink(text, timeout = 2000) {
        const start = Date.now();
        let anchor = null;
        while (!anchor && Date.now() - start < timeout) {
            const root = document.querySelector('nav.menu') || document;
            const span = Array.from(root.querySelectorAll('span.txt')).find(s => s.textContent?.includes(text));
            anchor = span ? span.closest('a') : null;
            if (!anchor) await new Promise(r => setTimeout(r, 10));
        }
        return anchor;
    }

    async function expandMarketing() {
        const btn = Array.from(document.querySelectorAll('button.group-btn')).find(b => b.textContent?.includes('Маркетинг'));
        if (btn) {
            const chev = btn.querySelector('.chev');
            const isOpen = !!chev && chev.classList.contains('open');
            if (!isOpen) {
                await fireEvent.click(btn);
                await new Promise(r => setTimeout(r, 0));
            }
        }
    }

    async function expandOps() {
        const btn = Array.from(document.querySelectorAll('button.group-btn')).find(b => b.textContent?.includes('Операции'));
        if (btn) {
            const chev = btn.querySelector('.chev');
            const isOpen = !!chev && chev.classList.contains('open');
            if (!isOpen) {
                await fireEvent.click(btn);
                await new Promise(r => setTimeout(r, 0));
            }
        }
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
        const hiddenKitchenEl = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes('Кухня'));
        const hiddenKitchen = !hiddenKitchenEl || getComputedStyle(hiddenKitchenEl).display === 'none';
        expect(hiddenKitchen).toBe(true);

        // COOK
        auth.roles = ['COOK'];
        auth.brandId = 1;
        auth.membershipId = 1;
        utils.unmount();
        try {
            localStorage.setItem('ui_sidebar_expanded', JSON.stringify({sklad: true, marketing: true, ops: true}));
        } catch (_) {
        }
        utils = renderSidebar();
        await new Promise(r => setTimeout(r, 10));
        await expandOps();
        await new Promise(r => setTimeout(r, 20));
        const visibleKitchen = await waitForMenuLink('Кухня');
        expect(visibleKitchen && getComputedStyle(visibleKitchen).display !== 'none').toBe(true);

        // ADMIN
        auth.roles = ['ADMIN'];
        auth.brandId = 1;
        auth.membershipId = 1;
        utils.unmount();
        try {
            localStorage.setItem('ui_sidebar_expanded', JSON.stringify({sklad: true, marketing: true, ops: true}));
        } catch (_) {
        }
        utils = renderSidebar();
        await new Promise(r => setTimeout(r, 10));
        await expandOps();
        await new Promise(r => setTimeout(r, 20));
        const adminKitchen = await waitForMenuLink('Кухня');
        expect(adminKitchen).toBeTruthy();

        // OWNER
        auth.roles = ['OWNER'];
        auth.brandId = 1;
        auth.membershipId = 1;
        utils.unmount();
        try {
            localStorage.setItem('ui_sidebar_expanded', JSON.stringify({sklad: true, marketing: true, ops: true}));
        } catch (_) {
        }
        utils = renderSidebar();
        await new Promise(r => setTimeout(r, 10));
        await expandOps();
        await new Promise(r => setTimeout(r, 20));
        const ownerKitchen = await waitForMenuLink('Кухня');
        expect(ownerKitchen).toBeTruthy();
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
        const hiddenCashierEl = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes('Касса'));
        const hiddenCashier = !hiddenCashierEl || getComputedStyle(hiddenCashierEl).display === 'none';
        expect(hiddenCashier).toBe(true);

        // CASHIER
        auth.roles = ['CASHIER'];
        auth.brandId = 1;
        auth.membershipId = 1;
        utils.unmount();
        try {
            localStorage.setItem('ui_sidebar_expanded', JSON.stringify({sklad: true, marketing: true, ops: true}));
        } catch (_) {
        }
        utils = renderSidebar();
        await new Promise(r => setTimeout(r, 10));
        await expandOps();
        await new Promise(r => setTimeout(r, 20));
        const visibleCashier = await waitForMenuLink('Касса');
        expect(visibleCashier && getComputedStyle(visibleCashier).display !== 'none').toBe(true);

        // ADMIN
        auth.roles = ['ADMIN'];
        auth.brandId = 1;
        auth.membershipId = 1;
        utils.unmount();
        try {
            localStorage.setItem('ui_sidebar_expanded', JSON.stringify({sklad: true, marketing: true, ops: true}));
        } catch (_) {
        }
        utils = renderSidebar();
        await new Promise(r => setTimeout(r, 10));
        await expandOps();
        await new Promise(r => setTimeout(r, 20));
        const adminCash = await waitForMenuLink('Касса');
        expect(adminCash).toBeTruthy();

        // OWNER
        auth.roles = ['OWNER'];
        auth.brandId = 1;
        auth.membershipId = 1;
        utils.unmount();
        try {
            localStorage.setItem('ui_sidebar_expanded', JSON.stringify({sklad: true, marketing: true, ops: true}));
        } catch (_) {
        }
        utils = renderSidebar();
        await new Promise(r => setTimeout(r, 10));
        await expandOps();
        await new Promise(r => setTimeout(r, 20));
        const ownerCash = await waitForMenuLink('Касса');
        expect(ownerCash).toBeTruthy();
    });

    it('shows Admin link only for ADMIN/OWNER', async () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT');

        auth.roles = ['CLIENT'];
        const ui = useUiStore();
        ui.isDesktop = true;
        ui.sidebarOpen = true;
        ui.sidebarCollapsed = false;
        let utils = renderSidebar();
        const hiddenAdmin = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes('Админ'));
        expect(hiddenAdmin && getComputedStyle(hiddenAdmin).display === 'none').toBe(true);

        auth.roles = ['ADMIN'];
        auth.brandId = 1;
        utils.unmount();
        utils = renderSidebar();
        const adminLink2 = await waitForMenuLink('Админ');
        expect(adminLink2).toBeTruthy();

        auth.roles = ['OWNER'];
        auth.brandId = 1;
        utils.unmount();
        utils = renderSidebar();
        const ownerAdmin = await waitForMenuLink('Админ');
        expect(ownerAdmin).toBeTruthy();
    });
});
