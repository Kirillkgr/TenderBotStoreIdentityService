import {defineStore} from 'pinia';
import * as productService from '../services/productService';

export const useProductStore = defineStore('product', {
    state: () => ({
        categories: [],
        products: [],
        selectedProduct: null,
        loading: false,
        error: null,
    }),

    actions: {
        async fetchCategories() {
            this.loading = true;
            try {
                const response = await productService.getCategories();
                this.categories = response.data;
            } catch (error) {
                console.error('Ошибка при загрузке категорий:', error);
            } finally {
                this.loading = false;
            }
        },

        async fetchProducts(categoryId = null) {
            this.loading = true;
            try {
                const response = await productService.getProducts(categoryId);
                this.products = response.data;
            } catch (error) {
                console.error('Ошибка при загрузке товаров:', error);
            } finally {
                this.loading = false;
            }
        },

        async fetchProductDetails(id) {
            this.loading = true;
            this.selectedProduct = null;
            try {
                const response = await productService.getProductDetails(id);
                this.selectedProduct = response.data;
            } catch (error) {
                console.error('Ошибка при загрузке информации о товаре:', error);
            } finally {
                this.loading = false;
            }
        },

        // ================= Новые действия для брендов/групп =================
        async fetchByBrandAndGroup(brandId, groupTagId = 0, visibleOnly = true) {
            this.loading = true;
            this.error = null;
            try {
                const res = await productService.getProductsByBrandAndGroup(brandId, groupTagId, visibleOnly);
                const arr = Array.isArray(res) ? res : (res?.data ?? res?.data?.data ?? []);
                this.products = arr || [];
                return this.products;
            } catch (error) {
                console.error('Ошибка при загрузке товаров по бренду/группе:', error);
                this.error = error.message || 'Не удалось загрузить товары';
                this.products = [];
                throw error;
            } finally {
                this.loading = false;
            }
        },

        // Публичная загрузка товаров для Главной (menu/v1)
        async fetchPublicByBrandAndGroup(brandId, groupTagId = 0, visibleOnly = true) {
            this.loading = true;
            this.error = null;
            try {
                const res = await productService.getPublicProductsByBrandAndGroup(brandId, groupTagId, visibleOnly);
                const arr = Array.isArray(res) ? res : (res?.data ?? res?.data?.data ?? []);
                this.products = arr || [];
                return this.products;
            } catch (error) {
                console.error('Ошибка (public) при загрузке товаров по бренду/группе:', error);
                this.error = error.message || 'Не удалось загрузить товары';
                this.products = [];
                throw error;
            } finally {
                this.loading = false;
            }
        },

        async create(productData) {
            try {
                const res = await productService.createProduct(productData);
                const created = res?.data ?? res;
                // Опционально: если товар в текущем уровне, добавим его в список
                // Оставляем вызывающей стороне решать, добавлять ли в текущий список
                return created;
            } catch (error) {
                console.error('Ошибка при создании товара:', error);
                throw error;
            }
        },

        async toggleVisibility(productId, visible) {
            try {
                await productService.patchVisibility(productId, visible);
                const idx = this.products.findIndex(p => p.id === productId);
                if (idx !== -1) this.products[idx].visible = visible;
            } catch (error) {
                console.error('Ошибка при смене видимости товара:', error);
                throw error;
            }
        },

        async move(productId, targetGroupTagId = 0) {
            try {
                await productService.moveProduct(productId, targetGroupTagId);
                // Если переместили из текущего уровня — удалим из локального списка
                this.products = this.products.filter(p => p.id !== productId);
            } catch (error) {
                console.error('Ошибка при перемещении товара:', error);
                throw error;
            }
        },

        async delete(productId) {
            try {
                await productService.deleteProduct(productId);
                this.products = this.products.filter(p => p.id !== productId);
            } catch (error) {
                console.error('Ошибка при удалении товара:', error);
                throw error;
            }
        },
    },
});
