import {beforeEach, describe, expect, it, vi} from 'vitest';
import {mount} from '@vue/test-utils';
import {createPinia, setActivePinia} from 'pinia';
import AppHeader from '@/components/AppHeader.vue';
import {useAuthStore} from '@/store/auth';
import {useCartStore} from '@/store/cart';

// cart store network already mocked by tests/setup.js (axios mock)

describe('AppHeader responsive/mobile behavior', () => {
  let pinia;

  beforeEach(() => {
    pinia = createPinia();
    setActivePinia(pinia);
  });

  function mountHeader(authenticated = false) {
    const auth = useAuthStore();
    // emulate auth through public API/state patch, not by writing computed flags
    if (authenticated) {
      if (typeof auth.setAccessToken === 'function') auth.setAccessToken('AT');
      auth.$patch({ user: { id: 1, username: 'Test' }, roles: ['USER'] });
    } else {
      auth.$patch({ user: null, roles: [] });
    }
    const cart = useCartStore();
    cart.items = [];
    cart.total = 0;
    // prevent network during mounted() cart initialization
    cart.fetchCart = vi.fn().mockResolvedValue({ items: [], total: 0 });

    return mount(AppHeader, {
      props: { isModalVisible: false },
      global: {
        plugins: [pinia],
        stubs: { transition: false }
      },
      attachTo: document.body
    });
  }

  async function waitForGone(selector, timeout = 1000) {
    const start = Date.now();
    // eslint-disable-next-line no-constant-condition
    while (true) {
      const el = document.body.querySelector(selector);
      if (!el) return;
      if (Date.now() - start > timeout) return;
      await new Promise(r => setTimeout(r, 10));
    }
  }

  it('shows avatar button and toggles guest menu on click (mobile widths)', async () => {
    const wrapper = mountHeader(false);
    // Simulate narrow viewport
    const prevWidth = window.innerWidth;
    window.innerWidth = 350;
    window.dispatchEvent(new Event('resize'));

    await wrapper.vm.$nextTick();
    const avatarBtn = document.body.querySelector('button.user-chip');
    expect(avatarBtn).toBeTruthy();

    // Guest: clicking avatar shows login/register menu
    avatarBtn.click();
    await wrapper.vm.$nextTick();
    const guestLogin = document.body.querySelector('.user-menu button.user-menu__item');
    expect(guestLogin?.textContent).toContain('Войти');

    // restore width
    window.innerWidth = prevWidth;
  });

  it('opens and closes QR modal, locking body scroll', async () => {
    const wrapper = mountHeader(true);
    // Click QR button
    const qrBtn = document.body.querySelector('.qr-btn');
    expect(qrBtn).toBeTruthy();
    qrBtn.click();
    await wrapper.vm.$nextTick();

    const overlay = document.body.querySelector('.qr-overlay');
    expect(overlay).toBeTruthy();
    expect(document.body.style.overflow).toBe('hidden');

    // Click overlay to close (transition is stubbed, but wait defensively)
    overlay.click();
    await wrapper.vm.$nextTick();
    await waitForGone('.qr-overlay');
    expect(document.body.querySelector('.qr-overlay')).toBeFalsy();
    expect(document.body.style.overflow).toBe('');
  });

  it('applies theme class on cycleTheme and reacts to media change (smoke)', async () => {
    const wrapper = mountHeader(true);
    const html = document.documentElement;
    // initial applyTheme called in onMounted
    expect(html.classList.contains('theme-light') || html.classList.contains('theme-dark')).toBe(true);

    // Find theme button and click to cycle
    const themeBtn = document.body.querySelector('.ttv-btn');
    expect(themeBtn).toBeTruthy();
    themeBtn.click();
    await wrapper.vm.$nextTick();
    expect(html.classList.contains('theme-light') || html.classList.contains('theme-dark')).toBe(true);
  });

  it('keeps avatar and nav-links inside viewport and clickable at 350px', async () => {
    const wrapper = mountHeader(true);
    const prevWidth = window.innerWidth;
    window.innerWidth = 350;
    window.dispatchEvent(new Event('resize'));
    await wrapper.vm.$nextTick();

    const avatarWrap = document.body.querySelector('.user-chip-wrap');
    const navLinks = document.body.querySelector('.nav-links');
    expect(avatarWrap).toBeTruthy();
    expect(navLinks).toBeTruthy();

    const vw = window.innerWidth;
    const vh = window.innerHeight || 768;
    const aRect = avatarWrap.getBoundingClientRect();
    const nRect = navLinks.getBoundingClientRect();
    // within viewport horizontally and vertically
    expect(aRect.left).toBeGreaterThanOrEqual(0);
    expect(aRect.right).toBeLessThanOrEqual(vw);
    expect(aRect.top).toBeGreaterThanOrEqual(0);
    expect(aRect.bottom).toBeLessThanOrEqual(vh);

    expect(nRect.left).toBeGreaterThanOrEqual(0);
    expect(nRect.right).toBeLessThanOrEqual(vw);
    expect(nRect.top).toBeGreaterThanOrEqual(0);
    expect(nRect.bottom).toBeLessThanOrEqual(vh);

    // clickable: avatar button exists and can be clicked to open menu
    const avatarBtn = document.body.querySelector('button.user-chip');
    expect(avatarBtn).toBeTruthy();
    avatarBtn.click();
    await wrapper.vm.$nextTick();
    const menu = document.body.querySelector('.user-menu');
    expect(menu).toBeTruthy();

    // restore
    window.innerWidth = prevWidth;
  });
});
