<template>
  <section class="inventory dark-surface">
    <header class="page-header">
      <h1>Поставщики</h1>
      <div class="page-controls">
        <input v-model="q" class="search" placeholder="Поиск (название, телефон, email, адрес)"/>
        <button
            v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
            class="btn primary"
            @click="onCreate"
        >Добавить поставщика
        </button>
      </div>
    </header>

    <!-- Warehouse-like tabs (UI focus) -->
    <div aria-label="Выбор склада" class="warehouse-tabs" role="tablist">
      <button
          v-for="t in tabs"
          :key="t.key"
          :aria-selected="activeTab === t.key ? 'true' : 'false'"
          :class="{ active: activeTab === t.key }"
          class="warehouse-tab"
          role="tab"
          @click="activeTab = t.key"
      >{{ t.label }}
      </button>
    </div>

    <section class="table-wrap">
      <table aria-describedby="table-desc" class="items-table">
        <caption id="table-desc" class="sr-only">Таблица поставщиков</caption>
        <thead>
        <tr>
          <th>#</th>
          <th>Название</th>
          <th>Телефон</th>
          <th>Email</th>
          <th>Адрес</th>
          <th>Действия</th>
        </tr>
        </thead>
        <tbody>
        <tr v-if="loading">
          <td class="muted" colspan="6">Загрузка…</td>
        </tr>
        <tr v-for="(s, idx) in filtered" :key="s.id" class="row">
          <td class="muted">{{ idx + 1 }}</td>
          <td>{{ s.name }}</td>
          <td>{{ s.phone || '—' }}</td>
          <td>{{ s.email || '—' }}</td>
          <td>{{ s.address || '—' }}</td>
          <td class="actions">
            <button
                v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
                class="btn small"
                @click="onEdit(s)"
            >✏️
            </button>
            <button
                v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
                class="btn small outline"
                @click="onDelete(s)"
            >🗑️
            </button>
          </td>
        </tr>
        <tr v-if="!loading && filtered.length === 0">
          <td class="muted" colspan="6">Нет данных</td>
        </tr>
        </tbody>
      </table>
      <div class="table-footer">
        <div class="pager">
          <button class="btn outline small" disabled>Назад</button>
          <span class="muted">Стр. 1 из 1</span>
          <button class="btn outline small" disabled>Вперёд</button>
        </div>
        <div class="perpage">
          <label>10
            <select disabled>
              <option>10</option>
              <option>25</option>
              <option>50</option>
            </select>
          </label>
        </div>
      </div>
    </section>

    <SupplierForm
        v-if="showForm"
        :supplier="editing"
        @cancel="onCancel"
        @save="onSave"
    />
  </section>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useInventoryStore} from '../../store/inventoryStore';
import SupplierForm from '../../components/inventory/SupplierForm.vue';

const store = useInventoryStore();
const loading = computed(() => store.suppliersLoading);
const q = ref('');

const tabs = [
  {key: 'central', label: 'Центральный'},
  {key: 'kitchen2', label: 'Кухня №2'},
  {key: 'w3', label: 'Склад 3'},
];
const activeTab = ref('central');

const showForm = ref(false);
const editing = ref(null);

const filtered = computed(() => {
  const query = q.value.trim().toLowerCase();
  const list = Array.isArray(store.suppliers) ? store.suppliers : [];
  if (!query) return list;
  return list.filter(x =>
      (x.name || '').toLowerCase().includes(query) ||
      (x.phone || '').toLowerCase().includes(query) ||
      (x.email || '').toLowerCase().includes(query) ||
      (x.address || '').toLowerCase().includes(query)
  );
});

async function load() {
  await store.fetchSuppliers();
}

function onCreate() {
  editing.value = null;
  showForm.value = true;
}

function onEdit(s) {
  editing.value = {...s};
  showForm.value = true;
}

async function onDelete(s) {
  if (!s?.id) return;
  if (!confirm(`Удалить поставщика "${s.name}"?`)) return;
  try {
    await store.deleteSupplier(s.id);
  } catch (e) {
    alert(e?.response?.data?.message || 'Ошибка удаления');
  }
}

async function onSave(payload) {
  try {
    if (payload.id) await store.updateSupplier(payload.id, payload);
    else await store.createSupplier(payload);
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
/***** Adopted from provided design (dark/light ready) *****/
.inventory {
  max-width: 1200px;
  margin: 24px auto;
  padding: 18px;
  border-radius: 8px
}

.dark-surface {
  background: transparent
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px
}

.page-header h1 {
  margin: 0;
  font-size: 20px
}

.page-controls {
  display: flex;
  gap: 8px;
  align-items: center
}

.search {
  background: #0b0b0b;
  border: 1px solid var(--border, #1f2933);
  padding: 8px 10px;
  border-radius: 8px;
  color: var(--muted, #9ca3af)
}

.btn {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.04);
  cursor: pointer;
  background: transparent;
  color: var(--muted, #9ca3af)
}

.btn.primary {
  background: var(--primary, #1f6feb);
  color: white;
  border: transparent
}

.btn.small {
  padding: 6px 8px;
  font-size: 13px
}

.btn.outline {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.06)
}

.table-wrap {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.04);
  border-radius: 8px;
  padding: 6px
}

.items-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 6px
}

.items-table th, .items-table td {
  padding: 14px 12px;
  text-align: left;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  font-size: 14px
}

.items-table thead th {
  font-weight: 600;
  background: transparent;
  color: var(--muted, #9ca3af)
}

.muted {
  color: var(--muted, #9ca3af)
}

.actions {
  display: flex;
  gap: 8px
}

.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px
}

.pager {
  display: flex;
  gap: 8px;
  align-items: center
}

.perpage select {
  margin-left: 8px;
  background: #0b0b0b;
  border: 1px solid var(--border, #1f2933);
  color: var(--muted, #9ca3af);
  padding: 6px;
  border-radius: 6px
}

/* Warehouse tabs */
.warehouse-tabs {
  display: flex;
  gap: 6px;
  margin: 12px 0 0
}

.warehouse-tab {
  background: var(--tab-bg, rgba(255, 255, 255, 0.02));
  border: 1px solid rgba(255, 255, 255, 0.06);
  padding: 8px 14px;
  border-radius: 10px 10px 0 0;
  color: var(--tab-text, #9ca3af);
  cursor: pointer;
  position: relative;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.25) inset;
  transition: background .15s ease, color .15s ease, transform .06s ease
}

.warehouse-tab:not(.active):hover {
  transform: translateY(-1px)
}

.warehouse-tab.active {
  background: var(--tab-active-bg, linear-gradient(180deg, #f7e8cf, #f2d8a8));
  color: var(--tab-active-text, #111827);
  border-color: rgba(0, 0, 0, 0.2);
  margin-bottom: -1px;
  box-shadow: none
}

/* Light overrides when parent sets body.light globally */
:global(body.light) .items-table th, :global(body.light) .items-table td {
  border-bottom: 1px solid #eef2f7
}
</style>
