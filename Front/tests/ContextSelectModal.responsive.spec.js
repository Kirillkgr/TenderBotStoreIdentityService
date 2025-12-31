import {beforeEach, describe, expect, it} from 'vitest'
import {mount} from '@vue/test-utils'
import {createPinia, setActivePinia} from 'pinia'
import ContextSelectModal from '@/components/modals/ContextSelectModal.vue'
import {useAuthStore} from '@/store/auth'

function mountModal(memberships = [], visible = true) {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.$patch({memberships})
    const wrapper = mount(ContextSelectModal, {
        props: {visible},
        global: {plugins: [pinia], stubs: {transition: false}},
        attachTo: document.body
    })
    return {wrapper, auth}
}

function assertInViewport(el) {
    const vw = window.innerWidth
    const vh = window.innerHeight || 768
    const r = el.getBoundingClientRect()
    expect(r.left).toBeGreaterThanOrEqual(0)
    expect(r.top).toBeGreaterThanOrEqual(0)
    expect(r.right).toBeLessThanOrEqual(vw)
    expect(r.bottom).toBeLessThanOrEqual(vh)
}

describe('ContextSelectModal responsive/centering', () => {
    beforeEach(() => {
        // cleanup between tests
        document.body.innerHTML = ''
    })

    it('is centered and within viewport at 350px', async () => {
        const prevW = window.innerWidth
        const prevH = window.innerHeight
        Object.defineProperty(window, 'innerWidth', {value: 350, configurable: true})
        Object.defineProperty(window, 'innerHeight', {value: 640, configurable: true})

        const {wrapper} = mountModal([
            {membershipId: 1, masterName: 'M1', brandName: 'B1'},
            {membershipId: 2, masterName: 'M2', brandName: 'B2'},
        ], true)
        await wrapper.vm.$nextTick();

        await wrapper.vm.$nextTick();
        const card = document.body.querySelector('.ctx-card')
        expect(card).toBeTruthy()
        assertInViewport(card)

        card.getBoundingClientRect()

        // restore
        Object.defineProperty(window, 'innerWidth', {value: prevW, configurable: true})
        Object.defineProperty(window, 'innerHeight', {value: prevH, configurable: true})
    })

    it('is centered and within viewport at 370px', async () => {
        const prevW = window.innerWidth
        const prevH = window.innerHeight
        Object.defineProperty(window, 'innerWidth', {value: 370, configurable: true})
        Object.defineProperty(window, 'innerHeight', {value: 640, configurable: true})

        const {wrapper} = mountModal([
            {membershipId: 1, masterName: 'M1', brandName: 'B1'}
        ], true)
        await wrapper.vm.$nextTick()

        const card = document.body.querySelector('.ctx-card')
        expect(card).toBeTruthy()
        assertInViewport(card)

        card.getBoundingClientRect()

        // restore
        Object.defineProperty(window, 'innerWidth', {value: prevW, configurable: true})
        Object.defineProperty(window, 'innerHeight', {value: prevH, configurable: true})
    })
})
