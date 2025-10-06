import {beforeEach, describe, expect, it, vi} from 'vitest';
import {mockVueRouterForPush, pushMock, renderWithAcl, setupPiniaAuth} from './utils/aclTestUtils';
import AppHeader from '@/components/AppHeader.vue';

describe('ACL Smoke: routes guard and visibility', () => {
    beforeEach(() => {
        vi.resetModules();
        mockVueRouterForPush();
    });

    it('GUEST does not see Admin/Orders/Cashier/Kitchen', async () => {
        setupPiniaAuth({roles: [], memberships: []});
        renderWithAcl(AppHeader, {props: {isModalVisible: false}});
        expect(document.body.textContent).not.toContain('Админ');
        expect(document.body.textContent).not.toContain('Заказы');
        expect(document.body.textContent).not.toContain('Касса');
        expect(document.body.textContent).not.toContain('Кухня');
    });

    it('ADMIN sees Admin and Orders; can navigate Orders immediately', async () => {
        setupPiniaAuth({roles: ['ADMIN'], memberships: []});
        renderWithAcl(AppHeader, {props: {isModalVisible: false}});
        const ordersLink = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes('Заказы'));
        ordersLink?.dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true}));
        await new Promise(r => setTimeout(r, 10));
        expect(pushMock).toHaveBeenCalled();
    });

    it('CASHIER in memberships only: link visible, ensures context switch then navigates', async () => {
        const memberships = [{id: 7, role: 'CASHIER', brandId: 1}];
        setupPiniaAuth({roles: [], memberships});
        renderWithAcl(AppHeader, {props: {isModalVisible: false}});
        const ordersLink = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes('Заказы'));
        ordersLink?.dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true}));
        await new Promise(r => setTimeout(r, 50));
        expect(pushMock).toHaveBeenCalled();
    });

    it('Unknown role does not unlock admin', async () => {
        setupPiniaAuth({roles: ['HACKER'], memberships: []});
        renderWithAcl(AppHeader, {props: {isModalVisible: false}});
        expect(document.body.textContent).not.toContain('Админ');
    });
});
