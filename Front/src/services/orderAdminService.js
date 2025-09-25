import apiClient from './api';

const orderAdminService = {
    listAccessibleOrders(page = 0, size = 20, sort = 'id,DESC', search, dateFrom, dateTo) {
        return apiClient.get(`/order/v1/orders`, {
            params: {page, size, sort, search, dateFrom, dateTo},
        });
    },
    listBrandOrders(brandId, page = 0, size = 20, sort = 'id,DESC') {
        return apiClient.get(`/order/v1/brand/${brandId}/orders`, {
            params: {page, size, sort},
        });
    },
    updateStatus(orderId, newStatus) {
        return apiClient.patch(`/order/v1/orders/${orderId}/status`, {newStatus});
    },
    sendMessage(orderId, text) {
        return apiClient.post(`/order/v1/orders/${orderId}/message`, {text});
    },
    getMessages(orderId) {
        return apiClient.get(`/order/v1/orders/${orderId}/messages`);
    },
};

export default orderAdminService;
export const ORDER_STATUSES = [
    'QUEUED',
    'PREPARING',
    'READY_FOR_PICKUP',
    'OUT_FOR_DELIVERY',
    'DELIVERED',
    'COMPLETED',
    'CANCELED',
];
