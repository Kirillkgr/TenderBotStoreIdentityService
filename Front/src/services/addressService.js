import apiClient from './api';

export const listAddresses = () => apiClient.get('/profile/v1/addresses');
export const createAddress = (payload) => apiClient.post('/profile/v1/addresses', payload);
export const deleteAddress = (id) => apiClient.delete(`/profile/v1/addresses/${id}`);
