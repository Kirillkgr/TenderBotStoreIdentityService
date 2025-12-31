import {beforeEach, describe, expect, it, vi} from 'vitest';
import {fireEvent} from '@testing-library/vue';
import {mockVueRouterForPush, pushMock, renderWithAcl, replaceMock, setupPiniaAuth} from './utils/aclTestUtils';
import AppHeader from '@/components/AppHeader.vue';
import Sidebar from '@/components/Sidebar.vue';
import {useUiStore} from '@/store/ui';

function click(el) {
    el?.dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true}));
}

describe('ACL Smoke: AppHeader links, context select, ensureRoleAndGo', () => {
    beforeEach(() => {
        vi.resetModules();
        mockVueRouterForPush();
        pushMock.mockClear();
        replaceMock.mockClear();
    });

    it('opens ContextSelectModal via header button and switches membership', async () => {
        const memberships = [
            {id: 1, role: 'CASHIER', brandId: 10, brandName: 'alpha'},
            {id: 2, role: 'COOK', brandId: 11, brandName: 'beta'}
        ];
        const auth = setupPiniaAuth({
            roles: [], memberships, extras: {
                async selectMembership(m) {
                    this.membershipId = m.id;
                    this.brandId = m.brandId ?? null;
                    const r = String(m.role || '').toUpperCase();
                    if (r && !this.roles.includes(r)) {
                        this.roles = [...this.roles, r];
                        this.$patch({roles: this.roles});
                    }
                }
            }
        });
        renderWithAcl(AppHeader, {props: {isModalVisible: false}});
        // open user menu, then click "Контексты" inside it
        const avatarBtn = document.querySelector('.user-chip');
        await fireEvent.click(avatarBtn);
        const btn = Array.from(document.querySelectorAll('.user-menu button'))
            .find(b => b.textContent?.includes('Контексты'));
        await fireEvent.click(btn);
        // click Choose on second membership
        const chooseButtons = Array.from(document.querySelectorAll('.ctx-list .btn-primary'));
        await fireEvent.click(chooseButtons[1]);
        await new Promise(r => setTimeout(r, 0));
        expect(auth.brandId).toBe(11);
    });

    it('shows Orders in Sidebar when role allows (CASHIER)', async () => {
        setupPiniaAuth({roles: ['CASHIER'], memberships: []});
        const ui = useUiStore();
        ui.isDesktop = true;
        ui.sidebarOpen = true;
        ui.sidebarCollapsed = false;
        renderWithAcl(Sidebar, {});
        const marketingBtn = Array.from(document.querySelectorAll('button.group-btn')).find(b => b.textContent?.includes('Маркетинг'));
        if (marketingBtn) await fireEvent.click(marketingBtn);
        const ordersLink = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes('Заказы'));
        expect(ordersLink && getComputedStyle(ordersLink).display !== 'none').toBe(true);
    });

    it('does not show Orders as visible when role is missing (guest)', async () => {
        setupPiniaAuth({roles: [], memberships: []});
        const ui = useUiStore();
        ui.isDesktop = true;
        ui.sidebarOpen = true;
        ui.sidebarCollapsed = false;
        renderWithAcl(Sidebar, {});
        const marketingBtn2 = Array.from(document.querySelectorAll('button.group-btn')).find(b => b.textContent?.includes('Маркетинг'));
        if (marketingBtn2) await fireEvent.click(marketingBtn2);
        const ordersLink = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes('Заказы'));
        const hiddenOrAbsent = !ordersLink || getComputedStyle(ordersLink).display === 'none';
        expect(hiddenOrAbsent).toBe(true);
    });
});
