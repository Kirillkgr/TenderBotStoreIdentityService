<template>
  <section class="inventory-page">
    <header class="page-header">
      <h1>Единицы измерения</h1>
      <div class="actions">
        <button class="btn primary" @click="onCreate">Добавить</button>
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
          <th>Короткое</th>
          <th style="width:1%">Действия</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(u, idx) in filtered" :key="u.id">
          <td>{{ idx + 1 }}</td>
          <td>{{ u.name }}</td>
          <td>{{ u.shortName || '—' }}</td>
          <td class="row-actions">
            <button class="btn" @click="onEdit(u)">✏️</button>
            <button class="btn danger" @click="onDelete(u)">🗑️</button>
          </td>
        </tr>
        <tr v-if="filtered.length === 0">
          <td class="muted" colspan="4">Нет данных</td>
        </tr>
        </tbody>
      </table>
    </div>

    <UnitForm
        v-if="showForm"
        :unit="editing"
        @cancel="onCancel"
        @save="onSave"
    />
  </section>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useInventoryStore} from '../../store/inventoryStore';
import UnitForm from '../../components/inventory/UnitForm.vue';

const store = useInventoryStore();
const loading = computed(() => store.unitsLoading);
const q = ref('');

const showForm = ref(false);
const editing = ref(null);

const filtered = computed(() => {
  const query = q.value.trim().toLowerCase();
  const list = Array.isArray(store.units) ? store.units : [];
  if (!query) return list;
  return list.filter(x => (x.name || '').toLowerCase().includes(query) || (x.shortName || '').toLowerCase().includes(query));
});

async function load() {
  await store.fetchUnits();
}

function onCreate() {
  editing.value = null;
  showForm.value = true;
}

function onEdit(u) {
  editing.value = {...u};
  showForm.value = true;
}

async function onDelete(u) {
  if (!u?.id) return;
  if (!confirm(`Удалить единицу "${u.name}"?`)) return;
  try {
    await store.deleteUnit(u.id);
  } catch (e) {
    alert(e?.response?.data?.message || 'Ошибка удаления');
  }
}

async function onSave(payload) {
  try {
    if (payload.id) await store.updateUnit(payload.id, {name: payload.name, shortName: payload.shortName});
    else await store.createUnit({name: payload.name, shortName: payload.shortName});
    showForm.value = false;
    editing.value = null;
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
