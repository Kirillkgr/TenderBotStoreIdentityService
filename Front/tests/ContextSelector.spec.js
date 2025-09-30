import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {mount} from '@vue/test-utils';
import {createPinia, setActivePinia} from 'pinia';
import {useAuthStore} from '@/store/auth';
import AppHeader from '@/components/AppHeader.vue';
import apiClient from '@/services/api';

function mountHeaderWithStore(memberships = []) {
    const pinia = createPinia();
    setActivePinia(pinia);
    const auth = useAuthStore();
    auth.memberships = memberships;
    auth.isRestoringSession = false;
    auth.setAccessToken('AT0');
    auth.membershipId = memberships[0] ? memberships[0].membershipId : null;
    auth.masterId = memberships[0]?.masterId || null;
    auth.brandId = memberships[0]?.brandId || null;
    auth.locationId = memberships[0]?.locationId || null;

    // mock selectMembership to simulate backend switchContext and update store
    auth.selectMembership = vi.fn(async (m) => {
        auth.membershipId = m.membershipId;
        auth.masterId = m.masterId || null;
        auth.brandId = m.brandId || null;
        auth.locationId = m.locationId || null;
        auth.setAccessToken('AT_ctx_' + m.membershipId);
    });

    const wrapper = mount(AppHeader, {
        props: {isModalVisible: true},
        global: {
            plugins: [pinia],
            stubs: {
                'router-link': {template: '<a><slot /></a>'}
            }
        },
        attachTo: document.body
    });
    return {wrapper, auth};
}

describe('Context selector in AppHeader.vue', () => {
    let wrapper;
    afterEach(() => {
        try {
            wrapper && wrapper.unmount();
        } catch (_) {
        }
        document.body.innerHTML = '';
    });

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders select with membership options and switches on change', async () => {
        const memberships = [
            {membershipId: 101, masterName: 'M1', brandName: 'B1', locationName: 'P1'},
            {membershipId: 202, masterName: 'M2', brandName: 'B2', locationName: 'P2'},
        ];
        const res = mountHeaderWithStore(memberships);
        wrapper = res.wrapper;
        const auth = res.auth;

        // wait render
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();

        const select = document.body.querySelector('select.ctx-select');
        expect(select).toBeTruthy();
        // Change to second membership
        select.value = String(202);
        select.dispatchEvent(new Event('change'));
        await wrapper.vm.$nextTick();

        expect(auth.selectMembership).toHaveBeenCalled();
        expect(auth.membershipId).toBe(202);
        expect(auth.accessToken).toBe('AT_ctx_202');
    });

    it('smoke: next request uses new X-Membership-Id without relogin', async () => {
        const memberships = [
            {membershipId: 1, masterId: 10, brandId: 100, locationId: 1000},
            {membershipId: 2, masterId: 20, brandId: 200, locationId: 2000},
        ];
        const res = mountHeaderWithStore(memberships);
        wrapper = res.wrapper;
        const auth = res.auth;
        await wrapper.vm.$nextTick();

        // switch to second membership via store mock directly
        await auth.selectMembership(memberships[1]);

        // override axios adapter to capture headers
        const prevAdapter = apiClient.defaults.adapter;
        apiClient.defaults.adapter = async (config) => {
            expect(config.headers['X-Membership-Id']).toBe('2');
            if (import.meta.env.DEV) {
                expect(config.headers['X-Master-Id']).toBe('20');
            }
            expect(config.headers.Authorization).toBe('Bearer AT_ctx_2');
            return {status: 200, statusText: 'OK', headers: {}, data: {ok: true}, config};
        };

        const r = await apiClient.get('/ping');
        expect(r.status).toBe(200);

        apiClient.defaults.adapter = prevAdapter;
    });
});
