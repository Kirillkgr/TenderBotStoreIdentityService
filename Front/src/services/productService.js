import apiClient from './api';

// ===================== Новые методы (auth/v1/products) =====================

// Создать товар (в корне или выбранной группе)
// body: { name, description, price, promoPrice, brandId, groupTagId (0 для корня), visible }
export const createProduct = (productData) => {
    return apiClient.post('/auth/v1/products', productData);
};

// Получить товары по бренду и группе (groupTagId=0 для корня)
export const getProductsByBrandAndGroup = (brandId, groupTagId = 0, visibleOnly = true) => {
    return apiClient.get(`/auth/v1/products/by-brand/${brandId}`, {
        params: {
            groupTagId: groupTagId ?? 0,
            visibleOnly: visibleOnly !== false
        }
    });
};

// Сменить видимость товара
export const patchVisibility = (productId, visible) => {
    return apiClient.patch(`/auth/v1/products/${productId}/visibility`, null, {
        params: { visible }
    });
};

// Переместить товар между группами (targetGroupTagId=0 для корня)
export const moveProduct = (productId, targetGroupTagId = 0) => {
    return apiClient.patch(`/auth/v1/products/${productId}/move`, null, {
        params: { targetGroupTagId }
    });
};

// Удалить товар (в архив)
export const deleteProduct = (productId) => {
    return apiClient.delete(`/auth/v1/products/${productId}`);
};

// ===================== Публичные методы (menu/v1) =====================

// Публичный: товары по бренду и группе (для Главной)
export const getPublicProductsByBrandAndGroup = (brandId, groupTagId = 0, visibleOnly = true) => {
    return apiClient.get(`/menu/v1/products/by-brand/${brandId}`, {
        params: {
            groupTagId: groupTagId ?? 0,
            visibleOnly: visibleOnly !== false
        }
    });
};
