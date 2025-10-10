import apiClient from '../api';

export const getWarehouses = () => apiClient.get('/auth/v1/inventory/warehouses');
export const createWarehouse = (payload) => apiClient.post('/auth/v1/inventory/warehouses', payload);
export const updateWarehouse = (id, payload) => apiClient.put(`/auth/v1/inventory/warehouses/${id}`, payload);
export const deleteWarehouse = (id) => apiClient.delete(`/auth/v1/inventory/warehouses/${id}`);
