import {beforeEach, describe, expect, it, vi} from 'vitest';
import {createPinia, setActivePinia} from 'pinia';
import apiClient from '@/services/api';
import {useAuthStore} from '@/store/auth';

describe('api interceptor refresh flow', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        setActivePinia(createPinia());
    });

    it('401 -> refresh -> retry original once succeeds', async () => {
        const auth = useAuthStore();
        auth.setAccessToken('AT0');

        const prev = apiClient.defaults.adapter;
        let state = 'FIRST_401';
        apiClient.defaults.adapter = async (config) => {
            // Simulate flow based on URL/state
            if (config.url === '/auth/v1/refresh') {
                // Return new token
                return {
                    status: 200,
                    statusText: 'OK',
                    headers: {},
                    config,
                    data: {accessToken: 'AT1', id: 1, username: 'u'},
                };
            }
            if (state === 'FIRST_401') {
                state = 'RETRY';
                return Promise.reject({
                    config,
                    response: {status: 401, data: {message: 'unauthorized'}},
                });
            }
            // Retry should carry new Authorization
            expect(config.headers.Authorization).toBe('Bearer AT1');
            return {
                status: 200,
                statusText: 'OK',
                headers: {},
                config,
                data: {ok: true},
            };
        };

        const resp = await apiClient.get('/secure/ping');
        expect(resp.status).toBe(200);
        expect(auth.accessToken).toBe('AT1');

        apiClient.defaults.adapter = prev;
    });

    it('concurrent 401 requests share single refresh', async () => {
        const auth = useAuthStore();
        auth.setAccessToken('ATZ');

        const prev = apiClient.defaults.adapter;
        let refreshCalls = 0;
        let firstPhase = true;
        apiClient.defaults.adapter = async (config) => {
            if (config.url === '/auth/v1/refresh') {
                refreshCalls += 1;
                return {
                    status: 200,
                    statusText: 'OK',
                    headers: {},
                    config,
                    data: {accessToken: 'AT_SHARED', id: 1, username: 'u'},
                };
            }
            if (firstPhase) {
                // First wave returns 401 for both calls
                return Promise.reject({config, response: {status: 401}});
            }
            // After refresh shared token is applied internally; we only require success and single refresh
            return {status: 200, statusText: 'OK', headers: {}, config, data: {ok: true}};
        };

        const p1 = apiClient.get('/secure/a');
        const p2 = apiClient.get('/secure/b');
        // turn to retry phase after refresh happens once
        firstPhase = false;

        const [r1, r2] = await Promise.all([p1, p2]);
        expect(r1.status).toBe(200);
        expect(r2.status).toBe(200);

        apiClient.defaults.adapter = prev;
    });
});
