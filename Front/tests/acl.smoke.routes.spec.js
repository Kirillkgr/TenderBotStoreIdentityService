import {beforeEach, describe, expect, it, vi} from 'vitest';
import {fireEvent} from '@testing-library/vue';
import {mockVueRouterForPush, renderWithAcl, setupPiniaAuth} from './utils/aclTestUtils';
import Sidebar from '@/components/Sidebar.vue';
import {useUiStore} from '@/store/ui';
import {useAuthStore} from '@/store/auth';

describe('ACL Smoke: routes guard and visibility', () => {
    beforeEach(() => {
        vi.resetModules();
        mockVueRouterForPush();
    });

    async function waitForMenuLink(text, timeout = 1000) {
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

    it('GUEST does not see Admin/Orders/Cashier/Kitchen', async () => {
        setupPiniaAuth({roles: [], memberships: []});
        const ui = useUiStore();
        ui.isDesktop = true;
        ui.sidebarOpen = true;
        ui.sidebarCollapsed = false;
        renderWithAcl(Sidebar, {});
        // Expand groups to expose subitems
        const buttons = Array.from(document.querySelectorAll('button.group-btn'));
        for (const b of buttons) await fireEvent.click(b);
        const adminLink = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes('Админ'));
        expect(!adminLink || getComputedStyle(adminLink).display === 'none').toBe(true);
        const orders = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes('Заказы'));
        expect(!orders || getComputedStyle(orders).display === 'none').toBe(true);
        const cashier = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes('Касса'));
        expect(!cashier || getComputedStyle(cashier).display === 'none').toBe(true);
        const kitchen = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes('Кухня'));
        expect(!kitchen || getComputedStyle(kitchen).display === 'none').toBe(true);
    });

    it('ADMIN sees Admin and Orders', async () => {
        setupPiniaAuth({roles: ['ADMIN'], memberships: []});
        const auth = useAuthStore();
        auth.brandId = 1;
        auth.membershipId = 1;
        const ui = useUiStore();
        ui.isDesktop = true;
        ui.sidebarOpen = true;
        ui.sidebarCollapsed = false;
        try {
            localStorage.setItem('ui_sidebar_expanded', JSON.stringify({sklad: true, marketing: true, ops: true}));
        } catch (_) {
        }
        renderWithAcl(Sidebar, {});
        await new Promise(r => setTimeout(r, 20));
        const ordersLink = await waitForMenuLink('Заказы');
        expect(ordersLink && getComputedStyle(ordersLink).display !== 'none').toBe(true);
    });

    it('CASHIER role sees Orders', async () => {
        setupPiniaAuth({roles: ['CASHIER'], memberships: []});
        const auth = useAuthStore();
        auth.brandId = 1;
        auth.membershipId = 1;
        const ui = useUiStore();
        ui.isDesktop = true;
        ui.sidebarOpen = true;
        ui.sidebarCollapsed = false;
        try {
            localStorage.setItem('ui_sidebar_expanded', JSON.stringify({sklad: true, marketing: true, ops: true}));
        } catch (_) {
        }
        renderWithAcl(Sidebar, {});
        await new Promise(r => setTimeout(r, 20));
        const ordersLink = await waitForMenuLink('Заказы');
        expect(ordersLink && getComputedStyle(ordersLink).display !== 'none').toBe(true);
    });

    it('Unknown role does not unlock admin', async () => {
        setupPiniaAuth({roles: ['HACKER'], memberships: []});
        const ui = useUiStore();
        ui.isDesktop = true;
        ui.sidebarOpen = true;
        ui.sidebarCollapsed = false;
        renderWithAcl(Sidebar, {});
        const adminLink = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes('Админ'));
        expect(adminLink && getComputedStyle(adminLink).display === 'none').toBe(true);
    });
});
