import apiClient from './api';

export const listPickupPoints = () => apiClient.get('/public/v1/pickup-points');
