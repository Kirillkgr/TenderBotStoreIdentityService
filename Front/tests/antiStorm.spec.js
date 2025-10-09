import {beforeEach, describe, expect, it, vi} from 'vitest';
import {createPinia, setActivePinia} from 'pinia';
import apiClient from '@/services/api';

// These tests validate the anti-storm logic: endpoints are temporarily disabled after failed retry
// and long-poll endpoint is never disabled.

describe('api anti-storm and endpoint disabling', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        setActivePinia(createPinia());
        // reset sessionStorage state
        try {
            sessionStorage.clear();
        } catch (_) {
        }
    });

    it('blocks request immediately if endpoint is marked disabled (request-side anti-storm)', async () => {
        const prev = apiClient.defaults.adapter;
        // Seed disabled map to simulate previously failed endpoint
        const key = 'disabled_endpoints';
        const now = Date.now();
        try {
            sessionStorage.setItem(key, JSON.stringify({'/secure/failing': now + 60000}));
        } catch (_) {
        }

        // Adapter would normally succeed, but request interceptor must block earlier
        apiClient.defaults.adapter = async (config) => ({
            status: 200,
            statusText: 'OK',
            headers: {},
            config,
            data: {ok: true}
        });

        const err = await apiClient.get('/secure/failing').catch(e => e);
        expect(err).toBeInstanceOf(Error);
        expect(err.code).toBe('ENDPOINT_DISABLED');

        apiClient.defaults.adapter = prev;
    });

    it('never disables long-poll endpoint', async () => {
        const prev = apiClient.defaults.adapter;
        let count = 0;
        apiClient.defaults.adapter = async (config) => {
            if (config.url.startsWith('/notifications/longpoll')) {
                count += 1;
                // return 403 to avoid refresh loop; still should not be disabled
                return Promise.reject({config, response: {status: 403}});
            }
            if (config.url === '/auth/v1/refresh') {
                return {
                    status: 200,
                    statusText: 'OK',
                    headers: {},
                    config,
                    data: {accessToken: 'ATL', id: 1, username: 'u'}
                };
            }
            return {status: 200, statusText: 'OK', headers: {}, config, data: {ok: true}};
        };

        await apiClient.get('/notifications/longpoll?cursor=1').catch(() => {
        });
        await apiClient.get('/notifications/longpoll?cursor=2').catch(() => {
        });

        // Ensure both calls reached adapter (not disabled)
        expect(count).toBe(2);

        apiClient.defaults.adapter = prev;
    });
});
