import apiClient from './api';

// ===================== Новые методы (auth/v1/products) =====================

// Создать товар (в корне или выбранной группе)
// body: { name, description, price, promoPrice, brandId, groupTagId (0 для корня), visible }
export const createProduct = (productData) => {
    return apiClient.post('/auth/v1/products', productData);
};

// Получить карточку товара по id (админ)
export const getProductById = (productId) => {
    return apiClient.get(`/auth/v1/products/${productId}`);
};

// Обновить карточку товара (админ)
// body: { name, description, price, promoPrice, visible }
export const updateProduct = (productId, payload) => {
    return apiClient.put(`/auth/v1/products/${productId}`, payload);
};

// Сменить бренд у товара (админ)
export const changeProductBrand = (productId, brandId) => {
    return apiClient.patch(`/auth/v1/products/${productId}/brand`, null, { params: { brandId } });
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

// Архив: список по бренду (админ)
export const getArchivedProductsByBrand = (brandId, params = {}) => {
    return apiClient.get('/auth/v1/products/archive', {
        params: { brandId, ...params }
    });
};

// Архив (пагинация): по бренду, Page DTO
export const getArchivedProductsByBrandPaged = (brandId, {page = 0, size = 25, sort = 'archivedAt,desc'} = {}) => {
    return apiClient.get('/auth/v1/products/archive/paged', {
        params: {brandId, page, size, sort}
    });
};

// Архив: восстановить товар (админ)
export const restoreFromArchive = (archiveId, targetGroupTagId = null) => {
    const config = targetGroupTagId != null ? { params: { targetGroupTagId } } : undefined;
    return apiClient.post(`/auth/v1/products/archive/${archiveId}/restore`, null, config);
};

// Архив: ручная очистка (опционально, только ADMIN)
export const purgeArchive = (olderThanDays = 90) => {
    return apiClient.delete('/auth/v1/products/archive/purge', { params: { olderThanDays } });
};

// Архив: удалить одну запись
export const deleteArchivedProduct = (archiveId) => {
    return apiClient.delete(`/auth/v1/products/archive/${archiveId}`);
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

// Админ: товары по бренду и группе, с возможностью включать скрытые
export const getAdminProductsByBrandAndGroup = (brandId, groupTagId = 0, visibleOnly = false) => {
    return apiClient.get(`/auth/v1/products/by-brand/${brandId}`, {
        params: {
            groupTagId: groupTagId ?? 0,
            visibleOnly: !!visibleOnly
        }
    });
};
// ��������� ����������� ������ (�������������� ����������)
export const uploadProductImage = (productId, file) => {
    const form = new FormData();
    form.append('file', file);
    return apiClient.post(`/auth/v1/products/${productId}/image`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
};
