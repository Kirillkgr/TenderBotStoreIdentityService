import apiClient from './api';
import {useAuthStore} from '@/store/auth';

export const checkout = (payload) => {
    try {
        const auth = useAuthStore();
        const headers = {};
        if (auth?.accessToken) headers['Authorization'] = `Bearer ${auth.accessToken}`;
        return apiClient.post('/checkout', payload, {headers});
    } catch (_) {
        return apiClient.post('/checkout', payload);
    }
};
export const myOrders = () => apiClient.get('/checkout/my');
export const updateOrderStatus = (orderId, status) => apiClient.patch(`/checkout/${orderId}/status`, {status});
