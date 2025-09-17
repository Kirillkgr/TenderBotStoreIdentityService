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
