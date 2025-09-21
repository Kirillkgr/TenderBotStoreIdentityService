import {defineStore} from 'pinia';
import * as productService from '../services/productService';

export const useProductStore = defineStore('product', {
    state: () => ({
        products: [],
        selectedProduct: null,
        loading: false,
        error: null,
    }),

    actions: {
        // Старые экшены удалены, используем новые by-brand/group и public-версии ниже

        // ================= Новые действия для брендов/групп =================
        async fetchByBrandAndGroup(brandId, groupTagId = 0, visibleOnly = true) {
            this.loading = true;
            this.error = null;
            try {
                // В админке используем админ-эндпоинт, чтобы при visibleOnly=false видеть скрытые товары
                const res = await productService.getAdminProductsByBrandAndGroup(brandId, groupTagId, visibleOnly);
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
                // Опционально: если товар в текущем уровне, добавим его в список
                // Оставляем вызывающей стороне решать, добавлять ли в текущий список
                return res?.data ?? res;
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

        // ================= Расширение для полной управляемости =================
        async getById(productId) {
            const res = await productService.getProductById(productId);
            return res?.data ?? res;
        },

        async update(productId, payload) {
            const res = await productService.updateProduct(productId, payload);
            const updated = res?.data ?? res;
            const idx = this.products.findIndex(p => p.id === productId);
            if (idx !== -1) this.products[idx] = { ...this.products[idx], ...updated };
            return updated;
        },

        async changeBrand(productId, brandId) {
            const res = await productService.changeProductBrand(productId, brandId);
            const updated = res?.data ?? res;
            const idx = this.products.findIndex(p => p.id === productId);
            if (idx !== -1) this.products[idx] = { ...this.products[idx], ...updated };
            return updated;
        },

        async getArchiveByBrand(brandId, params = {}) {
            const res = await productService.getArchivedProductsByBrand(brandId, params);
            return res?.data ?? res;
        },

        async restoreFromArchive(archiveId, targetGroupTagId = null) {
            const res = await productService.restoreFromArchive(archiveId, targetGroupTagId);
            return res?.data ?? res;
        },

        async purgeArchive(olderThanDays = 90) {
            const res = await productService.purgeArchive(olderThanDays);
            return res?.data ?? res;
        },

        async deleteArchived(archiveId) {
            const res = await productService.deleteArchivedProduct(archiveId);
            return res?.data ?? res;
        }
    },
});
