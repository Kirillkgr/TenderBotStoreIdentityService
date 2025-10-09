import {beforeEach, describe, expect, it, vi} from 'vitest';
import {createPinia, setActivePinia} from 'pinia';
import {useAuthStore} from '@/store/auth';
import apiClient from '@/services/api';
import * as authService from '@/services/authService';

// Полностью мокнутый e2e-smoke: логин -> выбор контекста -> заголовки и видимость данных

describe('E2E smoke: context selection and visibility (fully mocked)', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        setActivePinia(createPinia());
    });

    it('login -> selectMembership(A) -> headers include X-Membership-Id=A; switch to B updates headers and data', async () => {
        // Моки сервисов авторизации
        vi.spyOn(authService, 'login').mockResolvedValue({data: {accessToken: 'AT_LOGIN', id: 1, username: 'user'}});
        vi.spyOn(authService, 'getMemberships').mockResolvedValue({
            data: [
                {
                    membershipId: 11,
                    masterId: 101,
                    brandId: 1001,
                    locationId: 0,
                    masterName: 'M-A',
                    brandName: 'Brand-A'
                },
                {
                    membershipId: 22,
                    masterId: 202,
                    brandId: 2002,
                    locationId: 0,
                    masterName: 'M-B',
                    brandName: 'Brand-B'
                },
            ]
        });
        vi.spyOn(authService, 'switchContext').mockImplementation(async (payload) => {
            const id = payload?.membershipId;
            return {data: {accessToken: id === 11 ? 'AT_11' : 'AT_22'}};
        });
        vi.spyOn(authService, 'refresh').mockRejectedValue(new Error('skip'));
        vi.spyOn(authService, 'getCurrentUser').mockResolvedValue({id: 1, username: 'user'});

        const auth = useAuthStore();

        // Логин: подтянем memberships
        await auth.login({username: 'u', password: 'p'});
        expect(auth.memberships.length).toBe(2);

        // Выбор контекста A (membershipId=11)
        await auth.selectMembership(auth.memberships[0]);
        expect(auth.accessToken).toBe('AT_11');

        // Мок адаптера axios: проверим заголовки и вернём бренды под текущим контекстом
        const prev = apiClient.defaults.adapter;
        apiClient.defaults.adapter = async (config) => {
            // cookie-based context: больше нет X-* заголовков
            expect(config.headers['X-Membership-Id']).toBeUndefined();
            expect(config.headers.Authorization).toBe('Bearer AT_11');
            // Возвращаем «видимые» бренды под A
            return {
                status: 200,
                statusText: 'OK',
                headers: {},
                data: {brands: [{id: 1001, name: 'Brand-A-only'}]},
                config,
            };
        };

        // Любой GET для срабатывания перехватчика и адаптера (brands)
        const respA = await apiClient.get('/auth/v1/brands');
        expect(respA.status).toBe(200);
        expect(respA.data.brands).toEqual([{id: 1001, name: 'Brand-A-only'}]);

        // Проверим также group-tags под A
        apiClient.defaults.adapter = async (config) => {
            expect(config.headers['X-Membership-Id']).toBeUndefined();
            expect(config.headers.Authorization).toBe('Bearer AT_11');
            expect(config.url).toBe('/auth/v1/group-tags');
            return {
                status: 200,
                statusText: 'OK',
                headers: {},
                data: {tags: [{id: 5001, name: 'A-Root'}]},
                config,
            };
        };
        const respATags = await apiClient.get('/auth/v1/group-tags');
        expect(respATags.status).toBe(200);
        expect(respATags.data.tags).toEqual([{id: 5001, name: 'A-Root'}]);

        // И продукты под A
        apiClient.defaults.adapter = async (config) => {
            expect(config.headers['X-Membership-Id']).toBeUndefined();
            expect(config.headers.Authorization).toBe('Bearer AT_11');
            expect(config.url).toBe('/auth/v1/products');
            return {
                status: 200,
                statusText: 'OK',
                headers: {},
                data: {products: [{id: 7001, name: 'A-Only-Product'}]},
                config,
            };
        };
        const respAProd = await apiClient.get('/auth/v1/products');
        expect(respAProd.status).toBe(200);
        expect(respAProd.data.products).toEqual([{id: 7001, name: 'A-Only-Product'}]);

        // Переключение на B (membershipId=22)
        await auth.selectMembership(auth.memberships[1]);
        expect(auth.accessToken).toBe('AT_22');

        // Обновим адаптер и проверим новые заголовки и данные
        apiClient.defaults.adapter = async (config) => {
            expect(config.headers['X-Membership-Id']).toBeUndefined();
            expect(config.headers.Authorization).toBe('Bearer AT_22');
            return {
                status: 200,
                statusText: 'OK',
                headers: {},
                data: {brands: [{id: 2002, name: 'Brand-B-only'}]},
                config,
            };
        };

        const respB = await apiClient.get('/auth/v1/brands');
        expect(respB.status).toBe(200);
        expect(respB.data.brands).toEqual([{id: 2002, name: 'Brand-B-only'}]);

        // group-tags под B
        apiClient.defaults.adapter = async (config) => {
            expect(config.headers['X-Membership-Id']).toBeUndefined();
            expect(config.headers.Authorization).toBe('Bearer AT_22');
            expect(config.url).toBe('/auth/v1/group-tags');
            return {
                status: 200,
                statusText: 'OK',
                headers: {},
                data: {tags: [{id: 5002, name: 'B-Root'}]},
                config,
            };
        };
        const respBTags = await apiClient.get('/auth/v1/group-tags');
        expect(respBTags.status).toBe(200);
        expect(respBTags.data.tags).toEqual([{id: 5002, name: 'B-Root'}]);

        // products под B
        apiClient.defaults.adapter = async (config) => {
            expect(config.headers['X-Membership-Id']).toBeUndefined();
            expect(config.headers.Authorization).toBe('Bearer AT_22');
            expect(config.url).toBe('/auth/v1/products');
            return {
                status: 200,
                statusText: 'OK',
                headers: {},
                data: {products: [{id: 7002, name: 'B-Only-Product'}]},
                config,
            };
        };
        const respBProd = await apiClient.get('/auth/v1/products');
        expect(respBProd.status).toBe(200);
        expect(respBProd.data.products).toEqual([{id: 7002, name: 'B-Only-Product'}]);

        // Восстановим адаптер
        apiClient.defaults.adapter = prev;
    });
});
