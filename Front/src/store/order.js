import {defineStore} from 'pinia';
import * as orderService from '../services/orderService';

export const useOrderStore = defineStore('order', {
    state: () => ({
        orders: [],
        loading: false,
    }),

    actions: {
        async fetchOrders() {
            this.loading = true;
            try {
                const response = await orderService.getOrderHistory();
                this.orders = response.data;
            } catch (error) {
                console.error('Ошибка при загрузке истории заказов:', error);
                this.orders = [];
            } finally {
                this.loading = false;
            }
        },
    },
});
