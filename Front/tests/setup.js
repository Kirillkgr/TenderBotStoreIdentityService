// Vitest setup file
import {afterEach, vi} from 'vitest';
import {cleanup} from '@testing-library/vue';

// Ensure jsdom has a robust localStorage shim
if (!globalThis.localStorage) {
    const store = new Map();
    globalThis.localStorage = {
        getItem: (k) => (store.has(k) ? String(store.get(k)) : null),
        setItem: (k, v) => store.set(k, String(v)),
        removeItem: (k) => store.delete(k),
        clear: () => store.clear(),
        key: (i) => Array.from(store.keys())[i] || null,
        get length() {
            return store.size;
        }
    };
}

// Default fetch mock (can be overridden in tests)
if (!globalThis.fetch) {
    globalThis.fetch = vi.fn(() => Promise.resolve({ok: true, json: async () => ({})}));
}

// Mock vue-router globally to avoid creating a real router in unit tests
vi.mock('vue-router', async () => {
    const routerStub = {
        currentRoute: {value: {fullPath: '/'}},
        beforeEach: vi.fn(),
        afterEach: vi.fn(),
        replace: vi.fn(async () => {
        }),
        push: vi.fn(async () => {
        }),
        back: vi.fn(),
    };
    return {
        useRoute: () => ({name: 'Home', fullPath: '/', params: {}, query: {}}),
        useRouter: () => routerStub,
        createRouter: () => routerStub,
        createWebHistory: () => ({}),
    };
});

// Mock authService by default. refresh rejects to simulate no refresh cookie, so unauthenticated flows (router guards) behave predictably.
vi.mock('@/services/authService', async () => {
    return {
        refresh: vi.fn(async () => {
            throw new Error('no cookie');
        }),
        getCurrentUser: vi.fn(async () => ({id: 1, username: 'test'})),
        getMemberships: vi.fn(async () => ({data: []})),
        switchContext: vi.fn(async () => ({data: {accessToken: 'CTX_AT'}})),
        logout: vi.fn(async () => ({})),
        revokeToken: vi.fn(async () => ({})),
        revokeAllUserTokens: vi.fn(async () => ({})),
        login: vi.fn(async () => ({data: {accessToken: 'AT', refreshToken: 'RT'}})),
        register: vi.fn(async () => ({data: {accessToken: 'AT', refreshToken: 'RT'}})),
        checkUsername: vi.fn(async () => ({})),
    };
});

// Register a global no-op 'can' directive to silence warnings in components using v-can
try {
    const {config} = await import('@vue/test-utils');
    config.global.directives = config.global.directives || {};
    config.global.directives.can = {
        mounted() {
        },
        updated() {
        },
    };
} catch (_) {
}

afterEach(() => {
    cleanup();
});
