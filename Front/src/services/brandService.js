import apiClient from './api';
import * as authService from './authService';
import {useAuthStore} from '../store/auth';

// Получение всех брендов
export const getBrands = () => {
    return apiClient.get('/auth/v1/brands');
};

// Публичное получение брендов (для Главной)
export const getPublicBrands = () => {
    return apiClient.get('/menu/v1/brands');
};

// Минимальный список брендов: только id и domain (для быстрой проверки сабдомена)
export const getPublicBrandsMin = () => {
    return apiClient.get('/menu/v1/brands/min');
};

// Получение групп тегов для конкретного бренда
export const getTagGroupsByBrandId = (brandId) => {
    return apiClient.post('/menu/brand/tag-groups/by-brand', {
        brandId: brandId,
    });
};

// Создание нового бренда с авто-переключением контекста на созданный бренд
export const createBrand = async (payloadOrName) => {
    const payload = typeof payloadOrName === 'string'
        ? {name: payloadOrName}
        : (payloadOrName || {});
    const resp = await apiClient.post('/auth/v1/brands', payload);
    try {
        const brandId = resp?.data?.id;
        if (brandId) {
            const auth = useAuthStore();
            // Обновим memberships и найдём тот, что соответствует новому бренду
            const res = await authService.getMemberships();
            const list = Array.isArray(res?.data) ? res.data : [];
            auth.memberships = list;
            const m = list.find(x => String(x.brandId || x.brand?.id) === String(brandId));
            if (m) {
                await auth.selectMembership(m);
            }
        }
    } catch (_) {
    }
    return resp;
};

// Создание нового продукта
export const createProduct = (productData) => {
    return apiClient.post('/menu/products', productData);
};

// Обновление бренда
export const updateBrand = (brandId, payload) => {
    return apiClient.put(`/auth/v1/brands/${brandId}`, payload);
};

// Архивирование/удаление бренда
export const deleteBrand = (brandId) => {
    return apiClient.delete(`/auth/v1/brands/${brandId}`);
};
