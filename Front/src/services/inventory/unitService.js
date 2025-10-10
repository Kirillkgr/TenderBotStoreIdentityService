import apiClient from '../api';

export const getUnits = () => apiClient.get('/auth/v1/inventory/units');
export const createUnit = (payload) => apiClient.post('/auth/v1/inventory/units', payload);
export const updateUnit = (id, payload) => apiClient.put(`/auth/v1/inventory/units/${id}`, payload);
export const deleteUnit = (id) => apiClient.delete(`/auth/v1/inventory/units/${id}`);
