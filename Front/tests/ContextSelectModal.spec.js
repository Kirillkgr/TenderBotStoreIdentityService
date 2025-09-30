import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {mount} from '@vue/test-utils';
import {createPinia, setActivePinia} from 'pinia';
import {useAuthStore} from '@/store/auth';
import ContextSelectModal from '@/components/modals/ContextSelectModal.vue';

function mountWithStore(memberships = []) {
    const pinia = createPinia();
    setActivePinia(pinia);
    const store = useAuthStore();
    store.memberships = memberships;
    store.selectMembership = vi.fn().mockResolvedValue(void 0);
    const wrapper = mount(ContextSelectModal, {
        props: {visible: true},
        global: {
            plugins: [pinia],
            stubs: {transition: false}
        },
        attachTo: document.body
    });
    return {wrapper, store};
}

describe('ContextSelectModal.vue', () => {
    let wrapper;
    beforeEach(() => {
        vi.clearAllMocks();
    });
    afterEach(() => {
        try {
            wrapper && wrapper.unmount();
        } catch (_) {
        }
        // cleanup teleported nodes
        document.body.innerHTML = '';
    });

    it('renders memberships list', async () => {
        ({wrapper} = mountWithStore([
            {membershipId: 1, masterName: 'M1', brandName: 'B1'},
            {membershipId: 2, masterName: 'M2', brandName: 'B2'},
        ]));
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        const items = document.body.querySelectorAll('.ctx-item');
        expect(items.length).toBe(2);
    });

    it('selects membership on button click', async () => {
        const res = mountWithStore([{membershipId: 42, masterName: 'M'}]);
        wrapper = res.wrapper;
        const store = res.store;
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        const btn = document.body.querySelector('button.btn-primary');
        await btn.click();
        expect(store.selectMembership).toHaveBeenCalled();
    });
});
