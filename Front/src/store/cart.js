import {defineStore} from 'pinia';
import * as cartService from '../services/cartService';

export const useCartStore = defineStore('cart', {
    state: () => ({
        items: [],
        total: 0,
        loading: false,
        currentBrandId: null,
    }),

    actions: {
        _persistBrand(brandId) {
            try {
                localStorage.setItem('cart_brand_id', brandId != null ? String(brandId) : '');
            } catch {
            }
        },
        _hydrateBrand() {
            try {
                const v = localStorage.getItem('cart_brand_id');
                if (v && v !== '') this.currentBrandId = Number(v);
            } catch {
            }
        },
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
                // Синхронизируем бренд корзины с сервера, если он есть
                const srvBrand = data.currentBrandId != null ? Number(data.currentBrandId) : null;
                if (srvBrand != null) {
                    this.currentBrandId = srvBrand;
                    this._persistBrand(this.currentBrandId);
                } else {
                    // fallback: гидратация из localStorage
                    if (this.currentBrandId == null) this._hydrateBrand();
                }
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
                const res = await cartService.addToCart(productId, quantity);
                // После успешного добавления обновляем корзину, чтобы получить актуальные данные
                await this.fetchCart();
                return {cleared: false};
            } catch (error) {
                const status = error?.response?.status;
                const code = error?.response?.data?.code;
                if (status === 409 && code === 'CART_BRAND_CONFLICT') {
                    // Конфликт бренда: не очищаем автоматически, возвращаем флаг, чтобы UI принял решение
                    return {conflict: true, previousBrandId: error?.response?.data?.previousBrandId ?? null};
                }
                console.error('Ошибка при добавлении товара в корзину:', error);
                throw error;
            }
        },

        // Предпочтительный способ: передаем весь продукт; возвращаем флаги конфликта бренда
        async addProduct(product, quantity = 1) {
            if (!product) throw new Error('product is required');
            const result = await this.addItem(product.id ?? product.productId ?? productId, quantity);
            return result;
        },

        async clearServerCart() {
            try {
                await cartService.clearCart();
                this.clearCart();
                await this.fetchCart();
            } catch (e) {
                console.error('Не удалось очистить корзину на сервере', e);
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
            this.currentBrandId = null;
            this._persistBrand(null);
        }
    },
});
