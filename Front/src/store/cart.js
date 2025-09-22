import {defineStore} from 'pinia';
import * as cartService from '../services/cartService';

export const useCartStore = defineStore('cart', {
    state: () => ({
        items: [],
        total: 0,
        loading: false,
    }),

    actions: {
        async fetchCart() {
            this.loading = true;
            try {
                const response = await cartService.getCart();
                // 204 No Content or empty body -> пустая корзина
                const data = (response && typeof response.data === 'object') ? response.data : {};
                const items = Array.isArray(data.items) ? data.items : [];
                const total = typeof data.total === 'number' ? data.total : 0;
                this.items = items;
                this.total = total;
            } catch (error) {
                console.error('Ошибка при загрузке корзины:', error);
                this.items = [];
                this.total = 0;
            } finally {
                this.loading = false;
            }
        },

        async addItem(productId, quantity) {
            try {
                await cartService.addToCart(productId, quantity);
                // После успешного добавления обновляем корзину, чтобы получить актуальные данные
                await this.fetchCart();
            } catch (error) {
                console.error('Ошибка при добавлении товара в корзину:', error);
                throw error;
            }
        },

        async removeItem(cartItemId) {
            try {
                await cartService.removeFromCart(cartItemId);
                // Обновляем корзину после удаления
                await this.fetchCart();
            } catch (error) {
                console.error('Ошибка при удалении товара из корзины:', error);
                throw error;
            }
        },

        clearCart() {
            this.items = [];
            this.total = 0;
        }
    },
});
