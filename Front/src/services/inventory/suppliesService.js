import apiClient from '../api';

export const createSupply = (payload) => apiClient.post('/auth/v1/inventory/supplies', payload);
export const updateSupply = (id, payload) => apiClient.put(`/auth/v1/inventory/supplies/${id}`, payload);
export const postSupply = (id) => apiClient.post(`/auth/v1/inventory/supplies/${id}/post`);
export const cancelSupply = (id) => apiClient.post(`/auth/v1/inventory/supplies/${id}/cancel`);
