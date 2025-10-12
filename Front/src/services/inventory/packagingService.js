import apiClient from '../api';

export const getPackagings = () => apiClient.get('/auth/v1/inventory/packagings');
export const createPackaging = (payload) => apiClient.post('/auth/v1/inventory/packagings', payload);
export const updatePackaging = (id, payload) => apiClient.put(`/auth/v1/inventory/packagings/${id}`, payload);
export const deletePackaging = (id) => apiClient.delete(`/auth/v1/inventory/packagings/${id}`);
