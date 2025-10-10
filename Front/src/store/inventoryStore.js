import {defineStore} from 'pinia';
import {createUnit, deleteUnit, getUnits, updateUnit} from '../services/inventory/unitService';

export const useInventoryStore = defineStore('inventory', {
    state: () => ({
        units: [],
        unitsLoading: false,
        unitsError: null,
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
        async createUnit(payload) {
            await createUnit(payload);
            await this.fetchUnits();
        },
        async updateUnit(id, payload) {
            await updateUnit(id, payload);
            await this.fetchUnits();
        },
        async deleteUnit(id) {
            await deleteUnit(id);
            await this.fetchUnits();
        },
    },
});
