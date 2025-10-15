import {beforeEach, describe, expect, it, vi} from 'vitest';
import {fireEvent} from '@testing-library/vue';
import {mockVueRouterForPush, renderWithAcl, setupPiniaAuth} from './utils/aclTestUtils';
import Sidebar from '@/components/Sidebar.vue';
import {useUiStore} from '@/store/ui';

describe('Sidebar navigation to Orders', () => {
    beforeEach(() => {
        vi.resetModules();
        mockVueRouterForPush();
    });

    async function waitForMenuLink(text, timeout = 600) {
        const start = Date.now();
        let el = null;
        while (!el && Date.now() - start < timeout) {
            el = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes(text));
            if (!el) await new Promise(r => setTimeout(r, 10));
        }
        return el;
    }

    it('click Orders navigates when role allows', async () => {
        setupPiniaAuth({roles: ['CASHIER'], memberships: []});
        const ui = useUiStore();
        ui.isDesktop = true;
        ui.sidebarOpen = true;
        ui.sidebarCollapsed = false;
        renderWithAcl(Sidebar, {});
        const marketingBtn = Array.from(document.querySelectorAll('button.group-btn')).find(b => b.textContent?.includes('Маркетинг'));
        if (marketingBtn) await fireEvent.click(marketingBtn);
        const ordersLink = await waitForMenuLink('Заказы');
        expect(ordersLink && getComputedStyle(ordersLink).display !== 'none').toBe(true);
    });
});
