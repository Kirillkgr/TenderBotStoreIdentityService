import {defineStore} from 'pinia';
import {deleteUnit, getUnits, updateUnit} from '../services/inventory/unitService';
import {createSupplier, deleteSupplier, getSuppliers, updateSupplier} from '../services/inventory/supplierService';
import {createWarehouse, deleteWarehouse, getWarehouses, updateWarehouse} from '../services/inventory/warehouseService';

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
    },
});
