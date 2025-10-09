import {beforeEach, describe, expect, it} from 'vitest';
import {createPinia, setActivePinia} from 'pinia';
import apiClient from '@/services/api';
import {useAuthStore} from '@/store/auth';

describe('api interceptor headers', () => {
    beforeEach(() => {
        setActivePinia(createPinia());
    });

    it('adds Authorization header and uses withCredentials (no legacy X-* headers)', async () => {
        const store = useAuthStore();
        store.setAccessToken('AT');
        // legacy fields no longer used for headers
        store.membershipId = 10;
        store.masterId = 100;

        // Override adapter to capture final config (no network)
        const prevAdapter = apiClient.defaults.adapter;
        apiClient.defaults.adapter = async (config) => {
            expect(config.headers.Authorization).toBe('Bearer AT');
            // cookie-based context: no legacy X-* headers
            expect(config.headers['X-Membership-Id']).toBeUndefined();
            expect(config.headers['X-Master-Id']).toBeUndefined();
            // cookies are used, ensure axios sends them
            expect(config.withCredentials).toBe(true);
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
