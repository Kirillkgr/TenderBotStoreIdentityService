import {beforeEach, describe, expect, it} from 'vitest';
import {createPinia, setActivePinia} from 'pinia';
import apiClient from '@/services/api';
import {useAuthStore} from '@/store/auth';

describe('api interceptor headers', () => {
    beforeEach(() => {
        setActivePinia(createPinia());
    });

    it('adds Authorization and X-Membership-Id (and X-Master-Id in dev)', async () => {
        const store = useAuthStore();
        store.setAccessToken('AT');
        store.membershipId = 10;
        store.masterId = 100;

        // Override adapter to capture final config (no network)
        const prevAdapter = apiClient.defaults.adapter;
        apiClient.defaults.adapter = async (config) => {
            expect(config.headers.Authorization).toBe('Bearer AT');
            expect(config.headers['X-Membership-Id']).toBe('10');
            if (import.meta.env.DEV) {
                expect(config.headers['X-Master-Id']).toBe('100');
            }
            return {
                status: 200,
                statusText: 'OK',
                headers: {},
                config,
                data: {ok: true},
            };
        };

        const resp = await apiClient.get('/ping');
        expect(resp.status).toBe(200);
        expect(resp.data.ok).toBe(true);

        // restore adapter
        apiClient.defaults.adapter = prevAdapter;
    });
});
