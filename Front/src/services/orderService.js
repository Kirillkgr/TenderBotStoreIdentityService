import apiClient from './api';

// Оформить заказ
export const placeOrder = (orderDetails) => {
    return apiClient.post('/orders/create', orderDetails);
};

// Получить историю заказов пользователя
export const getOrderHistory = () => {
    return apiClient.get('/orders');
};
