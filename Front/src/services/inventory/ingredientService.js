import apiClient from '../api';

export const getIngredients = (params = {}) =>
    apiClient.get('/auth/v1/inventory/ingredients', {params});
export const createIngredient = (payload) => apiClient.post('/auth/v1/inventory/ingredients', payload);
export const updateIngredient = (id, payload) => apiClient.put(`/auth/v1/inventory/ingredients/${id}`, payload);
export const deleteIngredient = (id) => apiClient.delete(`/auth/v1/inventory/ingredients/${id}`);
