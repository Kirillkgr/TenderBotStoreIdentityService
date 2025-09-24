import apiClient from './api';

export const checkout = (payload) => apiClient.post('/checkout', payload);
export const myOrders = () => apiClient.get('/checkout/my');
export const updateOrderStatus = (orderId, status) => apiClient.patch(`/checkout/${orderId}/status`, {status});
