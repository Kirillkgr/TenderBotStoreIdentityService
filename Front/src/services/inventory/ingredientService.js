import apiClient from '../api';

export const getIngredients = () => apiClient.get('/auth/v1/inventory/ingredients');
export const createIngredient = (payload) => apiClient.post('/auth/v1/inventory/ingredients', payload);
export const updateIngredient = (id, payload) => apiClient.put(`/auth/v1/inventory/ingredients/${id}`, payload);
export const deleteIngredient = (id) => apiClient.delete(`/auth/v1/inventory/ingredients/${id}`);
