import apiClient from '../api';

// Legacy helper used by WarehousesView
export const getWarehouseStock = (warehouseId) =>
    apiClient.get('/auth/v1/inventory/stock', {params: {warehouseId}});

// New BL3-11 helpers
export const listStock = ({warehouseId = null, ingredientId = null} = {}) =>
    apiClient.get('/auth/v1/inventory/stock', {
        params: {
            ...(warehouseId ? {warehouseId} : {}),
            ...(ingredientId ? {ingredientId} : {}),
        },
    });

export const increaseStock = ({ingredientId, warehouseId, qty}) =>
    apiClient.post('/auth/v1/inventory/stock/increase', {ingredientId, warehouseId, qty});

export const decreaseStock = ({ingredientId, warehouseId, qty}) =>
    apiClient.post('/auth/v1/inventory/stock/decrease', {ingredientId, warehouseId, qty});
