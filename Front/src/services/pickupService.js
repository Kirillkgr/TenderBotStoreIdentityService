import apiClient from './api';

export const listPickupPoints = (brandId) => apiClient.get(`/brand/${brandId}/pickup-points`);
