// Vitest setup file
import {afterEach, vi} from 'vitest';
import {cleanup} from '@testing-library/vue';

// Ensure jsdom has localStorage
if (!globalThis.localStorage) {
    const store = new Map();
    globalThis.localStorage = {
      removeItem: (k) => store.delete(k),
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

afterEach(() => {
    cleanup();
});
