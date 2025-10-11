import apiClient from '../api';

export const getSuppliers = () => apiClient.get('/auth/v1/inventory/suppliers');
export const createSupplier = (payload) => apiClient.post('/auth/v1/inventory/suppliers', payload);
export const updateSupplier = (id, payload) => apiClient.put(`/auth/v1/inventory/suppliers/${id}`, payload);
export const deleteSupplier = (id) => apiClient.delete(`/auth/v1/inventory/suppliers/${id}`);
