import apiClient from './api';

// Оформить заказ
export const placeOrder = (orderDetails) => {
    return apiClient.post('/orders/create', orderDetails);
};

// Получить историю заказов пользователя
export const getOrderHistory = () => {
    return apiClient.get('/order/v1/my');
};

// Админ: получить заказы с пагинацией/фильтрами (OWNER/ADMIN)
export const getAdminOrders = (params = {}) => {
    return apiClient.get('/order/v1/orders', {params});
};
