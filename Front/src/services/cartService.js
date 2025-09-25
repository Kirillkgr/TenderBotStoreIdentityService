import apiClient from './api';

// Добавить товар в корзину
export const addToCart = (productId, quantity) => {
    return apiClient.post('/cart/add', {productId, quantity});
};

// Получить содержимое корзины
export const getCart = () => {
    return apiClient.get('/cart');
};

// Удалить товар из корзины
export const removeFromCart = (cartItemId) => {
    return apiClient.delete(`/cart/remove/${cartItemId}`);
};

// Обновить количество позиции в корзине
export const updateCartItemQuantity = (cartItemId, quantity) => {
    return apiClient.patch(`/cart/item/${cartItemId}`, {quantity});
};

// Очистить корзину на сервере
export const clearCart = () => apiClient.delete('/cart/clear');
