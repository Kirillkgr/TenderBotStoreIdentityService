import apiClient from './api';

const orderClientService = {
    sendMessage(orderId, text) {
        return apiClient.post(`/order/v1/orders/${orderId}/client-message`, {text});
    },
    myOrders() {
        return apiClient.get('/order/v1/my');
    },
};

export default orderClientService;
