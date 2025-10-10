<template>
  <section class="inventory-page">
    <header class="page-header">
      <h1>Склады</h1>
      <div class="actions">
        <button class="btn primary" @click="onCreate">Добавить склад</button>
        <input v-model="q" placeholder="Поиск по названию" type="text"/>
      </div>
    </header>

    <div v-if="loading" class="loading">Загрузка…</div>
    <div v-else>
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
            <button class="btn" @click="onEdit(w)">✏️</button>
            <button class="btn danger" @click="onDelete(w)">🗑️</button>
          </td>
        </tr>
        <tr v-if="filtered.length === 0">
          <td class="muted" colspan="3">Нет данных</td>
        </tr>
        </tbody>
      </table>
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
import {
  createWarehouse,
  deleteWarehouse,
  getWarehouses,
  updateWarehouse
} from '../../services/inventory/warehouseService';
import WarehouseForm from '../../components/inventory/WarehouseForm.vue';

const items = ref([]);
const loading = ref(false);
const error = ref(null);
const q = ref('');

const showForm = ref(false);
const editing = ref(null);

const filtered = computed(() => {
  const query = q.value.trim().toLowerCase();
  if (!query) return items.value;
  return items.value.filter(x => x.name.toLowerCase().includes(query));
});

async function load() {
  loading.value = true;
  error.value = null;
  try {
    const {data} = await getWarehouses();
    items.value = Array.isArray(data) ? data : [];
  } catch (e) {
    error.value = e?.message || 'Ошибка загрузки';
  } finally {
    loading.value = false;
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
    await deleteWarehouse(w.id);
    await load();
  } catch (e) {
    alert(e?.response?.data?.message || 'Ошибка удаления');
  }
}

async function onSave(payload) {
  try {
    if (payload.id) {
      await updateWarehouse(payload.id, {name: payload.name});
    } else {
      await createWarehouse({name: payload.name});
    }
    showForm.value = false;
    editing.value = null;
    await load();
  } catch (e) {
    alert(e?.response?.data?.message || 'Ошибка сохранения');
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
</style>
