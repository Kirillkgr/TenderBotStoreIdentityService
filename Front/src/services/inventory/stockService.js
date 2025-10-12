import apiClient from '../api';

export const getWarehouseStock = (warehouseId) =>
    apiClient.get('/auth/v1/inventory/stock', {params: {warehouseId}});
