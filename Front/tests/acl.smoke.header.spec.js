import {beforeEach, describe, expect, it, vi} from 'vitest';
import {mockVueRouterForPush, pushMock, renderWithAcl, replaceMock, setupPiniaAuth} from './utils/aclTestUtils';
import AppHeader from '@/components/AppHeader.vue';

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

    it('renders context select when memberships exist and switches on change', async () => {
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

        const select = document.querySelector('select.ctx-select');
        expect(select).toBeTruthy();
        // switch to option 2
        select.value = String(memberships[1].id);
        select.dispatchEvent(new Event('change'));
        // ensure brandId updated
        await new Promise(r => setTimeout(r, 0));
        expect(auth.brandId).toBe(11);
    });

    it('ensureRoleAndGo waits roles then navigates (CASHIER present only in memberships)', async () => {
        const memberships = [{id: 3, role: 'CASHIER', brandId: 1}];
        setupPiniaAuth({
            roles: [], memberships, extras: {
                async selectMembership(m) {
                    this.membershipId = m.id;
                    this.brandId = m.brandId ?? null;
                    if (!this.roles.includes('CASHIER')) {
                        this.roles = [...this.roles, 'CASHIER'];
                        this.$patch({roles: this.roles});
                    }
                }
            }
        });
        renderWithAcl(AppHeader, {props: {isModalVisible: false}});

        const ordersLink = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes('Заказы'));
        click(ordersLink);
        await new Promise(r => setTimeout(r, 50));
        expect(pushMock).toHaveBeenCalled();
    });

    it('ensureRoleAndGo times out still tries to navigate, guards may block later', async () => {
        // roles list contains no target, and selectMembership does not patch roles
        const memberships = [{id: 5, role: 'CASHIER', brandId: 1}];
        const auth = setupPiniaAuth({
            roles: [], memberships, extras: {
                selectMembership: async () => {
                }
            }
        });
        renderWithAcl(AppHeader, {props: {isModalVisible: false}});

        const ordersLink = Array.from(document.querySelectorAll('a')).find(a => a.textContent?.includes('Заказы'));
        click(ordersLink);
        await new Promise(r => setTimeout(r, 1700)); // > timeout inside ensureRoleAndGo
        expect(pushMock).toHaveBeenCalled();
    });
});
