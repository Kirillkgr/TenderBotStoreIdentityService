import apiClient from './api';

// Получение всех брендов
export const getBrands = () => {
    return apiClient.get('/auth/v1/brands');
};

// Публичное получение брендов (для Главной)
export const getPublicBrands = () => {
    return apiClient.get('/menu/v1/brands');
};

// Получение групп тегов для конкретного бренда
export const getTagGroupsByBrandId = (brandId) => {
    return apiClient.post('/menu/brand/tag-groups/by-brand', {
        brandId: brandId,
    });
};

// Создание нового бренда
export const createBrand = (brandName) => {
    return apiClient.post('/auth/v1/brands', { name: brandName });
};

// Создание нового продукта
export const createProduct = (productData) => {
    return apiClient.post('/menu/products', productData);
};
