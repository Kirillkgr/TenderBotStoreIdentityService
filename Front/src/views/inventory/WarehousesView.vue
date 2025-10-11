<template>
  <section class="inventory-page">
    <header class="page-header">
      <h1>Склады</h1>
      <div class="actions">
        <router-link :to="{ name: 'Suppliers' }" class="btn">Поставщики</router-link>
        <router-link :to="{ name: 'Units' }" class="btn">Единицы</router-link>
        <input v-model="q" placeholder="Поиск по названию" type="text"/>
        <button v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }" class="btn primary"
                @click="onCreate">Добавить склад
        </button>
      </div>
    </header>

    <!-- Вкладки складов (выбор активного склада) -->
    <div v-if="!loading" class="warehouse-tabs">
      <button
          v-for="w in items"
          :key="w.id"
          :class="['tab', { active: selectedWarehouseId === w.id }]"
          :title="w.name"
          @click="selectedWarehouseId = w.id"
      >
        {{ w.name }}
      </button>
      <span v-if="items.length === 0" class="muted">Складов пока нет</span>
    </div>

    <div v-if="loading" class="loading">Загрузка…</div>
    <div v-else class="grid">
      <!-- Левая колонка: таблица складов -->
      <table class="table">
        <thead>
        <tr>
          <th>#</th>
          <th>Название</th>
          <th style="width: 1%">Действия</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(w, idx) in filtered" :key="w.id">
          <td>{{ idx + 1 }}</td>
          <td>{{ w.name }}</td>
          <td class="row-actions">
            <button v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }" class="btn"
                    @click="onEdit(w)">✏️
            </button>
            <button v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }" class="btn danger"
                    @click="onDelete(w)">🗑️
            </button>
          </td>
        </tr>
        <tr v-if="filtered.length === 0">
          <td class="muted" colspan="3">Нет данных</td>
        </tr>
        </tbody>
      </table>

      <!-- Правая колонка: шаблон будущего списка ингредиентов по складу -->
      <section class="warehouse-inventory">
        <div class="panel">
          <h3>
            {{ selectedWarehouse ? `Товары на складе: ${selectedWarehouse.name}` : 'Выберите склад' }}
          </h3>
          <p v-if="selectedWarehouse" class="muted">
            Здесь будет список ингредиентов для выбранного склада (BL3-08). Будут фильтры, поиск и операции перемещения.
          </p>
          <p v-else class="muted">
            Выберите склад во вкладках сверху, чтобы увидеть товары.
          </p>
          <div class="pill-actions">
            <router-link :to="{ name: 'Suppliers' }" class="btn pill">➜ Поставщики</router-link>
            <router-link :to="{ name: 'Units' }" class="btn pill">➜ Единицы измерения</router-link>
          </div>
        </div>
      </section>
    </div>

    <WarehouseForm
        v-if="showForm"
        :warehouse="editing"
        @cancel="onCancel"
        @save="onSave"
    />
  </section>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useInventoryStore} from '../../store/inventoryStore';
import WarehouseForm from '../../components/inventory/WarehouseForm.vue';

const store = useInventoryStore();
const items = computed(() => store.warehouses);
const loading = computed(() => store.warehousesLoading);
const error = computed(() => store.warehousesError);
const q = ref('');
const selectedWarehouseId = ref(null);

const showForm = ref(false);
const editing = ref(null);

const filtered = computed(() => {
  const query = q.value.trim().toLowerCase();
  if (!query) return items.value;
  return items.value.filter(x => x.name.toLowerCase().includes(query));
});

const selectedWarehouse = computed(() =>
    (items.value || []).find(w => w.id === selectedWarehouseId.value) || null
);

async function load() {
  try {
    await store.fetchWarehouses();
    if (!selectedWarehouseId.value && store.warehouses.length > 0) {
      selectedWarehouseId.value = store.warehouses[0].id;
    }
  } catch (_) {
  }
}

function onCreate() {
  editing.value = null;
  showForm.value = true;
}

function onEdit(w) {
  editing.value = {...w};
  showForm.value = true;
}

async function onDelete(w) {
  if (!w?.id) return;
  if (!confirm(`Удалить склад "${w.name}"?`)) return;
  try {
    await store.deleteWarehouse(w.id);
  } catch (e) {
    alert(e?.response?.data?.message || error.value || 'Ошибка удаления');
  }
}

async function onSave(payload) {
  try {
    if (payload.id) {
      await store.updateWarehouse(payload.id, {name: payload.name});
    } else {
      await store.createWarehouse({name: payload.name});
    }
    showForm.value = false;
    editing.value = null;
  } catch (e) {
    alert(e?.response?.data?.message || error.value || 'Ошибка сохранения');
  }
}

function onCancel() {
  showForm.value = false;
  editing.value = null;
}

onMounted(load);
</script>

<style scoped>
.inventory-page {
  padding: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.warehouse-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 8px 0 12px;
}

.warehouse-tabs .tab {
  padding: 6px 10px;
  border: 1px solid #d1d5db;
  border-radius: 16px;
  background: #fff;
  cursor: pointer;
}

.warehouse-tabs .tab.active {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table th, .table td {
  border-bottom: 1px solid #e5e7eb;
  padding: 8px;
  text-align: left;
}

.row-actions {
  display: flex;
  gap: 6px;
}

.btn {
  padding: 6px 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}

.btn.pill {
  border-radius: 999px;
}

.btn.primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}

.btn.danger {
  background: #ef4444;
  color: #fff;
  border-color: #ef4444;
}

.loading {
  padding: 16px;
}

.muted {
  color: #6b7280;
  text-align: center;
}

input[type="text"] {
  padding: 6px 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
}

.warehouse-inventory .panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  background: #fafafa;
}
</style>
