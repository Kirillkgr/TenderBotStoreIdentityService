import {beforeEach, describe, expect, it, vi} from 'vitest';
import {createPinia, setActivePinia} from 'pinia';
import {useAuthStore} from '@/store/auth';
import * as authService from '@/services/authService';

vi.mock('@/services/authService', () => ({
    getMemberships: vi.fn(),
    switchContext: vi.fn(),
    login: vi.fn().mockResolvedValue({data: {accessToken: 'AT_LOGIN', username: 'user'}}),
    register: vi.fn(),
    checkUsername: vi.fn(),
    logout: vi.fn(),
    getCurrentUser: vi.fn(),
    refresh: vi.fn(),
}));

function setupStore() {
    setActivePinia(createPinia());
    const store = useAuthStore();
    // avoid router errors in tests
    store.fetchProfile = vi.fn();
    return store;
}

describe('auth store context flow', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        // jsdom CustomEvent polyfill sometimes needed
        if (typeof window.CustomEvent !== 'function') {
            window.CustomEvent = function (event, params) {
                const evt = document.createEvent('CustomEvent');
                evt.initCustomEvent(event, params?.bubbles, params?.cancelable, params?.detail);
                return evt;
            };
        }
    });

    it('login() with single membership auto-selects and switches context', async () => {
        authService.getMemberships.mockResolvedValue({data: [{membershipId: 1, masterId: 100}]});
        authService.switchContext.mockResolvedValue({data: {accessToken: 'AT_CTX'}});
        const store = setupStore();

        await store.login({username: 'u', password: 'p'});

        // accessToken replaced by switchContext
        expect(store.accessToken).toBe('AT_CTX');
        expect(store.membershipId).toBe(1);
        expect(authService.switchContext).toHaveBeenCalledWith({membershipId: 1, brandId: null, locationId: null});
    });

    it('login() with multiple memberships dispatches open-context-modal and does not auto-select', async () => {
        const dispatchSpy = vi.spyOn(window, 'dispatchEvent');
        authService.getMemberships.mockResolvedValue({
            data: [
                {membershipId: 1, masterId: 100},
                {membershipId: 2, masterId: 200},
            ]
        });
        const store = setupStore();

        await store.login({username: 'u', password: 'p'});

        expect(store.memberships.length).toBe(2);
        expect(store.membershipId).toBeNull();
        expect(dispatchSpy).toHaveBeenCalled();
    });

    it('restoreSession() re-selects saved membership if available', async () => {
        authService.getMemberships.mockResolvedValue({data: [{membershipId: 7, masterId: 77}]});
        authService.switchContext.mockResolvedValue({data: {accessToken: 'AT_CTX2'}});

        const store = setupStore();
        // simulate backend refresh success via authService
        authService.refresh.mockResolvedValue({data: {accessToken: 'AT_REFRESH'}});

        // save previously selected id
        window.localStorage.setItem('selected_membership_id', '7');

        const ok = await store.restoreSession();
        expect(ok).toBe(true);
        expect(store.accessToken).toBe('AT_CTX2');
        expect(store.membershipId).toBe(7);
    });
});
