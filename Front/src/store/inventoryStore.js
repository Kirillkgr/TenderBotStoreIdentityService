import {defineStore} from 'pinia';
import {deleteUnit, getUnits, updateUnit} from '../services/inventory/unitService';
import {createSupplier, deleteSupplier, getSuppliers, updateSupplier} from '../services/inventory/supplierService';
import {createWarehouse, deleteWarehouse, getWarehouses, updateWarehouse} from '../services/inventory/warehouseService';
import {
    createIngredient,
    deleteIngredient,
    getIngredients,
    updateIngredient
} from '../services/inventory/ingredientService';
import {getWarehouseStock} from '../services/inventory/stockService';
import {createPackaging, deletePackaging, getPackagings, updatePackaging} from '../services/inventory/packagingService';

export const useInventoryStore = defineStore('inventory', {
    state: () => ({
        units: [],
        unitsLoading: false,
        unitsError: null,
        suppliers: [],
        suppliersLoading: false,
        suppliersError: null,
        warehouses: [],
        warehousesLoading: false,
        warehousesError: null,
        ingredients: [],
        ingredientsLoading: false,
        ingredientsError: null,
        packagings: [],
        packagingsLoading: false,
        packagingsError: null,
        warehouseStock: [],
        warehouseStockLoading: false,
        warehouseStockError: null,
    }),
    actions: {
        async fetchUnits() {
            this.unitsLoading = true;
            this.unitsError = null;
            try {
                const {data} = await getUnits();
                this.units = Array.isArray(data) ? data : [];
            } catch (e) {
                this.unitsError = e?.response?.data?.message || e?.message || 'Ошибка загрузки единиц измерения';
                throw e;
            } finally {
                this.unitsLoading = false;
            }
        },
        async updateUnit(id, payload) {
            await updateUnit(id, payload);
            await this.fetchUnits();
        },
        async deleteUnit(id) {
            await deleteUnit(id);
            await this.fetchUnits();
        },
        async fetchSuppliers() {
            this.suppliersLoading = true;
            this.suppliersError = null;
            try {
                const {data} = await getSuppliers();
                this.suppliers = Array.isArray(data) ? data : [];
            } catch (e) {
                this.suppliersError = e?.response?.data?.message || e?.message || 'Ошибка загрузки поставщиков';
                throw e;
            } finally {
                this.suppliersLoading = false;
            }
        },
        async createSupplier(payload) {
            await createSupplier(payload);
            await this.fetchSuppliers();
        },
        async updateSupplier(id, payload) {
            await updateSupplier(id, payload);
            await this.fetchSuppliers();
        },
        async deleteSupplier(id) {
            await deleteSupplier(id);
            await this.fetchSuppliers();
        },
        async fetchWarehouses() {
            this.warehousesLoading = true;
            this.warehousesError = null;
            try {
                const {data} = await getWarehouses();
                this.warehouses = Array.isArray(data) ? data : [];
            } catch (e) {
                this.warehousesError = e?.response?.data?.message || e?.message || 'Ошибка загрузки складов';
                throw e;
            } finally {
                this.warehousesLoading = false;
            }
        },
        async createWarehouse(payload) {
            await createWarehouse(payload);
            await this.fetchWarehouses();
        },
        async updateWarehouse(id, payload) {
            await updateWarehouse(id, payload);
            await this.fetchWarehouses();
        },
        async deleteWarehouse(id) {
            await deleteWarehouse(id);
            await this.fetchWarehouses();
        },

        // Ingredients
        async fetchIngredients() {
            this.ingredientsLoading = true;
            this.ingredientsError = null;
            try {
                const {data} = await getIngredients();
                this.ingredients = Array.isArray(data) ? data : [];
            } catch (e) {
                this.ingredientsError = e?.response?.data?.message || e?.message || 'Ошибка загрузки ингредиентов';
                throw e;
            } finally {
                this.ingredientsLoading = false;
            }
        },
        async createIngredient(payload) {
            await createIngredient(payload);
            await this.fetchIngredients();
        },
        async updateIngredient(id, payload) {
            await updateIngredient(id, payload);
            await this.fetchIngredients();
        },
        async deleteIngredient(id) {
            await deleteIngredient(id);
            await this.fetchIngredients();
        },
        async createIngredientReturning(payload) {
            const {data} = await createIngredient(payload);
            // поддерживаем кэш, но возвращаем объект
            try {
                await this.fetchIngredients();
            } catch (_) {
            }
            return data;
        },

        // Packagings
        async fetchPackagings() {
            this.packagingsLoading = true;
            this.packagingsError = null;
            try {
                const {data} = await getPackagings();
                this.packagings = Array.isArray(data) ? data : [];
            } catch (e) {
                this.packagingsError = e?.response?.data?.message || e?.message || 'Ошибка загрузки фасовок';
                throw e;
            } finally {
                this.packagingsLoading = false;
            }
        },
        async createPackaging(payload) {
            await createPackaging(payload);
            await this.fetchPackagings();
        },
        async updatePackaging(id, payload) {
            await updatePackaging(id, payload);
            await this.fetchPackagings();
        },
        async deletePackaging(id) {
            await deletePackaging(id);
            await this.fetchPackagings();
        },

        // Warehouse stock
        async fetchWarehouseStock(warehouseId) {
            if (!warehouseId) {
                this.warehouseStock = [];
                return;
            }
            this.warehouseStockLoading = true;
            this.warehouseStockError = null;
            try {
                const {data} = await getWarehouseStock(Number(warehouseId));
                this.warehouseStock = Array.isArray(data) ? data : [];
            } catch (e) {
                this.warehouseStockError = e?.response?.data?.message || e?.message || 'Ошибка загрузки остатков склада';
                throw e;
            } finally {
                this.warehouseStockLoading = false;
            }
        },
    },
});
