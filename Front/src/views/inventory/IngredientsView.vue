.btn.icon {
width: 34px;
height: 34px;
padding: 0;
display: inline-flex;
align-items: center;
justify-content: center;
font-size: 18px;
line-height: 1;
border-radius: 8px;
}

.divider {
display: inline-block;
width: 1px;
height: 28px;
background: #d1d5db;
margin: 0 8px;
}
<template>
  <section class="inventory-page">
    <header class="page-header">
      <div class="actions">
        <button
            v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
            aria-label="Добавить ингредиент"
            class="btn primary icon"
            title="Добавить ингредиент"
            @click="onCreate"
        >+
        </button>
        <span aria-hidden="true" class="divider"></span>
        <input v-model="q" placeholder="Поиск по названию" type="text"/>
        <select v-model.number="unitFilter">
          <option :value="0">Все единицы</option>
          <option v-for="u in units" :key="u.id" :value="u.id">{{ u.name }}</option>
        </select>
        <select v-model.number="warehouseFilter" @change="reloadByWarehouse">
          <option :value="0">Все склады</option>
          <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
        </select>
      </div>
    </header>

    <div v-if="loading" class="loading">Загрузка…</div>
    <div v-else>
      <div class="table-wrap">
      <table class="table">
        <thead>
        <tr>
          <th>#</th>
          <th>Название</th>
          <th>Ед.</th>
          <th>Склад</th>
          <th>Кол-во</th>
          <th>Последняя поставка</th>
          <th style="width: 120px">Действия</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(x, idx) in filtered" :key="x.id" :class="{ 'low-stock': hasSupply(x) && isLowQty(x) }">
          <td>{{ idx + 1 }}</td>
          <td>{{ x.name }}</td>
          <td>{{ x.unitName || unitName(x.unitId) }}</td>
          <td>{{ x.warehouseName || warehouseName(x.warehouseId) }}</td>
          <td>{{ x.quantity ?? '—' }}</td>
          <td>{{ x.lastSupplyDate ? x.lastSupplyDate.substring(0, 10) : '—' }}</td>
          <td class="row-actions">
            <button
                v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
                class="btn"
                @click="onEdit(x)"
            >✏️
            </button>
            <button
                v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
                class="btn danger"
                @click="onDelete(x)"
            >🗑️
            </button>
          </td>
        </tr>
        <tr v-if="filtered.length === 0">
          <td class="muted" colspan="5">Нет данных</td>
        </tr>
        </tbody>
      </table>
      </div>
    </div>

    <IngredientForm
        v-if="showForm"
        :ingredient="editing"
        :units="units"
        :packagings="packagings"
        :selectedWarehouseId="selectedWarehouseIdForForm"
        :warehouses="warehouses"
        @cancel="onCancel"
        @save="onSave"
    />
  </section>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useInventoryStore} from '@/store/inventoryStore.js';
import IngredientForm from '../../components/inventory/IngredientForm.vue';

const store = useInventoryStore();
const loading = computed(() => store.ingredientsLoading || store.unitsLoading);
const error = computed(() => store.ingredientsError);
const items = computed(() => store.ingredients || []);
const units = computed(() => store.units || []);
const warehouses = computed(() => store.warehouses || []);
const packagings = computed(() => store.packagings || []);

const q = ref('');
const unitFilter = ref(0);
const warehouseFilter = ref(0);

const filtered = computed(() => {
  const query = q.value.trim().toLowerCase();
  return (items.value || [])
      .filter(x => !unitFilter.value || x.unitId === unitFilter.value)
      .filter(x => !query || (x.name || '').toLowerCase().includes(query));
});

const showForm = ref(false);
const editing = ref(null);
const LOW_STOCK_THRESHOLD = 5;

function unitName(id) {
  const u = (units.value || []).find(u => u.id === id);
  return u ? (u.shortName || u.name) : '—';
}

function warehouseName(id) {
  const w = (warehouses.value || []).find(w => w.id === id);
  return w ? w.name : '—';
}

function hasSupply(x) {
  return !!(x && x.lastSupplyDate);
}

function isLowQty(x) {
  const q = Number(x?.quantity ?? 0);
  return Number.isFinite(q) && q < LOW_STOCK_THRESHOLD;
}

async function load() {
  try {
    await Promise.all([
      store.fetchUnits(),
      store.fetchWarehouses(),
      store.fetchPackagings(),
    ]);
    // По умолчанию показываем все склады
    warehouseFilter.value = 0;
    await reloadByWarehouse();
  } catch (e) {
    // noop; ошибки показываются через стор/алерты ниже
  }
}

const selectedWarehouseIdForForm = ref(0);

function onCreate() {
  editing.value = null;
  // по умолчанию выбираем первый склад, если есть
  const first = (warehouses.value || [])[0];
  selectedWarehouseIdForForm.value = first?.id || 0;
  showForm.value = true;
}

function onEdit(x) {
  editing.value = {...x};
  selectedWarehouseIdForForm.value = x?.warehouseId || 0;
  showForm.value = true;
}

async function reloadByWarehouse() {
  const wid = Number(warehouseFilter.value) || 0;
  if (wid > 0) {
    await store.fetchIngredients({warehouseId: wid});
  } else {
    await store.fetchIngredients({allWarehouses: true});
  }
}

async function onDelete(x) {
  if (!x?.id) return;
  if (!confirm(`Удалить ингредиент "${x.name}"?`)) return;
  try {
    await store.deleteIngredient(x.id);
  } catch (e) {
    alert(e?.response?.data?.message || error.value || 'Ошибка удаления');
  }
}

async function onSave(payload) {
  try {
    const base = {
      name: payload.name,
      unitId: payload.unitId,
      // warehouseId используется для начального прихода/операций со складом
      warehouseId: payload.warehouseId || null,
      packageSize: payload.packageSize ?? null,
      notes: payload.notes || null,
    };
    if (payload.id) {
      const prev = editing.value ? {
        wid: editing.value.warehouseId,
        qty: Number(editing.value.quantity || 0)
      } : {wid: null, qty: 0};
      const newWid = Number(payload.warehouseId || 0) || null;
      await store.updateIngredient(payload.id, base);

      // если сменили склад и есть что переносить — переведем фактическое текущее количество со старого склада на новый
      if (prev.wid && newWid && newWid !== prev.wid && prev.qty > 0) {
        try {
          // Получим актуальный остаток для пары (ингредиент, старый склад)
          await store.fetchStock({ingredientId: payload.id, warehouseId: prev.wid});
          const row = (store.stockRows || [])[0];
          const currentQty = Number(row?.quantity || 0);
          if (currentQty > 0) {
            await store.decreaseStock({ingredientId: payload.id, warehouseId: prev.wid, qty: currentQty});
            await store.increaseStock({ingredientId: payload.id, warehouseId: newWid, qty: currentQty});
          }
        } catch (_) {
          // если не удалось получить остаток — не пытаемся переносить, чтобы избежать ошибочного списания
        }
      }

      // применим корректировку количества, если указано (±) — уже на новом выбранном складе
      const delta = Number(payload.adjustQty || 0);
      if (delta && newWid) {
        if (delta > 0) {
          await store.increaseStock({ingredientId: payload.id, warehouseId: newWid, qty: delta});
        } else {
          await store.decreaseStock({ingredientId: payload.id, warehouseId: newWid, qty: Math.abs(delta)});
        }
      }
    } else {
      // передаем initialQty для создания начального остатка на складе (делает бэкенд)
      const createPayload = {...base, initialQty: payload.initialQty || 0};
      await store.createIngredient(createPayload);
    }
    showForm.value = false;
    editing.value = null;
    // перезагрузим список ингредиентов по текущему фильтру склада
    await reloadByWarehouse();
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
.low-stock {
  background-color: #fff7cc; /* светлая тема */
}

@media (prefers-color-scheme: dark) {
  .low-stock {
    background-color: rgba(255, 214, 10, 0.18); /* полупрозрачный янтарный */
    border-left: 3px solid #ffd54f; /* акцент для читаемости */
  }
}
</style>

<style scoped>
.inventory-page {
  padding: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: flex-start; /* тулбар слева */
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
  table-layout: fixed; /* фиксированная раскладка для сжатия колонок */
}

.table-wrap {
  overflow: hidden; /* без горизонтального скролла */
}

.table th, .table td {
  border-bottom: 1px solid #e5e7eb;
  padding: 8px;
  text-align: left;
  white-space: nowrap; /* одна строка */
  overflow: hidden; /* обрезать лишнее */
  text-overflow: ellipsis; /* троеточие */
}

/* В ячейке действий показываем кнопки полностью */
.table td.row-actions {
  overflow: visible;
  text-overflow: clip;
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

select {
  padding: 6px 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
}

input[type="text"] {
  padding: 6px 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
}

@media (max-width: 640px) {
  .actions {
    gap: 6px;
  }

  .table th, .table td {
    padding: 6px;
    font-size: 13px;
  }
}
</style>
