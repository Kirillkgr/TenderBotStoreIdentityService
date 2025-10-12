<template>
  <section class="inventory-page">
    <header class="page-header">
      <h1>Склад: {{ selectedWarehouse?.name || '—' }}</h1>
      <div class="page-controls">
        <input v-model="search" class="search" placeholder="Поиск (id, клиент, бренд)"/>
        <select v-model.number="selectedWarehouseId" aria-label="Выбор склада">
          <option v-for="w in items" :key="w.id" :value="w.id">{{ w.name }}</option>
        </select>
        <input v-model="dateFilter" type="date"/>
        <button class="btn primary" @click="openIngredientCreate">Добавить ингредиент</button>
      </div>
    </header>

    <!-- Вкладки складов + '+' -->
    <div v-if="!loading" class="warehouse-tabs-wrap">
      <div class="warehouse-tabs">
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
      <button
          v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
          class="btn small"
          title="Добавить склад"
          @click="onCreate"
      >＋
      </button>
    </div>

    <div v-if="loading" class="loading">Загрузка…</div>
    <div v-else>
      <!-- Таблица остатков по выбранному складу -->
      <section class="warehouse-inventory">
        <div class="panel">
          <div class="panel-header">
            <h3>
              {{ selectedWarehouse ? `Ингредиенты на складе: ${selectedWarehouse.name}` : 'Выберите склад' }}
            </h3>
            <div class="panel-actions">
              <button
                  v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
                  :disabled="!selectedWarehouse"
                  class="btn"
                  @click="openSupplyModal"
              >Добавить поставку
              </button>
              <button
                  v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
                  :disabled="!selectedWarehouse"
                  class="btn primary"
                  @click="openIngredientCreate"
              >Добавить ингредиент
              </button>
            </div>
          </div>

          <div v-if="!selectedWarehouse" class="muted">Выберите склад во вкладках сверху</div>
          <div v-else>
            <div v-if="stockLoading" class="loading">Загрузка остатков…</div>
            <div v-else>
              <table class="items-table">
                <thead>
                <tr>
                  <th>ID</th>
                  <th>Название</th>
                  <th>Тек. кол-во</th>
                  <th>Ед. изм.</th>
                  <th>Срок годности</th>
                  <th>Последняя поставка</th>
                  <th>Последнее использование</th>
                  <th>Поставщик</th>
                  <th>Категория</th>
                  <th>Действия</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="(row, idx) in pagedRows" :key="row.ingredientId || row.id || idx"
                    :class="rowClass(row)" class="row">
                  <td class="muted">{{ row.ingredientId ?? row.id ?? '—' }}</td>
                  <td>{{ row.name || '—' }}</td>
                  <td>{{ row.quantity ?? '—' }}</td>
                  <td>{{ row.unitName || unitShort(row.unitId) }}</td>
                  <td>{{ row.expiryDate || '—' }}</td>
                  <td>{{ row.lastSupplyDate || '—' }}</td>
                  <td>{{ row.lastUseDate || '—' }}</td>
                  <td>{{ row.supplierName || '—' }}</td>
                  <td>{{ row.categoryName || '—' }}</td>
                  <td class="actions">
                    <button class="btn small" @click="editIngredient(row)">Изменить</button>
                  </td>
                </tr>
                <tr v-if="(filteredRows.length || 0) === 0">
                  <td class="muted" colspan="10">На складе нет ингредиентов</td>
                </tr>
                </tbody>
              </table>

              <div class="table-footer">
                <div class="pager">
                  <button :disabled="page===1" class="btn outline small" @click="prevPage">Назад</button>
                  <span class="muted">Стр. {{ page }} из {{ totalPages }}</span>
                  <button :disabled="page===totalPages" class="btn outline small" @click="nextPage">Вперёд</button>
                </div>
                <div class="perpage">
                  <label>{{ perPage }}
                    <select v-model.number="perPage">
                      <option :value="10">10</option>
                      <option :value="25">25</option>
                      <option :value="50">50</option>
                    </select>
                  </label>
                </div>
              </div>

              <footer class="legend">
                <div><span class="chip low"></span> Мало на складе</div>
                <div><span class="chip expired"></span> Просрочено</div>
              </footer>
            </div>
          </div>

          <div class="pill-actions">
            <router-link :to="{ name: 'Suppliers' }" class="btn pill">➜ Поставщики</router-link>
            <router-link :to="{ name: 'Units' }" class="btn pill">➜ Единицы измерения</router-link>
          </div>
        </div>
      </section>
    </div>

    <WarehouseForm
        v-if="showForm"
        @cancel="onCancel"
        @save="onSave"
    />

    <IngredientForm
        v-if="showIngredientForm"
        :ingredient="ingredientEditing"
        :packagings="packagings"
        :selectedWarehouseId="selectedWarehouseId"
        :units="units"
        :warehouses="items"
        @cancel="closeIngredientForm"
        @save="saveIngredient"
    />

    <SupplyModal
        v-if="showSupply"
        :ingredients="ingredientsForModal"
        :loading="suppliersLoading"
        :packagings="packagings"
        :suppliers="suppliers"
        :units="units"
        :warehouse-id="selectedWarehouseId"
        @cancel="closeSupplyModal"
        @save="saveSupply"
    />
  </section>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useInventoryStore} from '../../store/inventoryStore';
import WarehouseForm from '../../components/inventory/WarehouseForm.vue';
import IngredientForm from '../../components/inventory/IngredientForm.vue';
import {useToast} from 'vue-toastification';
import {createSupply, postSupply} from '../../services/inventory/suppliesService';
import SupplyModal from '../../components/inventory/SupplyModal.vue';

const suppliersLoading = computed(() => store.suppliersLoading);

const store = useInventoryStore();
const items = computed(() => store.warehouses);
const loading = computed(() => store.warehousesLoading);
const error = computed(() => store.warehousesError);
const units = computed(() => store.units);
const packagings = computed(() => store.packagings);
const suppliers = computed(() => store.suppliers);
const warehouseStock = computed(() => store.warehouseStock);
const stockLoading = computed(() => store.warehouseStockLoading);
const toast = useToast();
const q = ref(''); // поиск по списку складов слева
const search = ref(''); // поиск по таблице остатков
const dateFilter = ref('');
const selectedWarehouseId = ref(null);

const showForm = ref(false);
const editing = ref(null);
const showIngredientForm = ref(false);
const ingredientEditing = ref(null);
const showSupply = ref(false);

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
    await Promise.all([
      store.fetchUnits(),
      store.fetchPackagings(),
      store.fetchWarehouses(),
      store.fetchSuppliers(),
      store.fetchIngredients(),
    ]);
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
  try {
    if (!confirm(`Удалить склад "${w.name}"?`)) return;
    await store.deleteWarehouse(w.id);
    toast.success('Склад удалён');
  } catch (e) {
    toast.error(e?.response?.data?.message || error.value || 'Ошибка удаления');
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
    toast.success('Сохранено');
  } catch (e) {
    toast.error(e?.response?.data?.message || error.value || 'Ошибка сохранения');
  }
}

function onCancel() {
  showForm.value = false;
  editing.value = null;
}

onMounted(load);

// Подгружаем остатки при смене выбранного склада
watch(selectedWarehouseId, async (id) => {
  try {
    if (id) await store.fetchWarehouseStock(id);
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Не удалось загрузить остатки склада');
  }
});

function openIngredientCreate() {
  ingredientEditing.value = null;
  showIngredientForm.value = true;
}

function editIngredient(row) {
  if (!row?.ingredientId && !row?.id) return;
  const id = row.ingredientId || row.id;
  const ing = (store.ingredients || []).find(x => x.id === id);
  ingredientEditing.value = ing ? {...ing} : {id};
  showIngredientForm.value = true;
}

function closeIngredientForm() {
  showIngredientForm.value = false;
  ingredientEditing.value = null;
}

async function saveIngredient(payload) {
  try {
    let created = null;
    if (payload.id) {
      await store.updateIngredient(payload.id, payload);
      created = {id: payload.id};
    } else {
      created = await store.createIngredientReturning(payload);
    }
    toast.success('Ингредиент сохранён');
    showIngredientForm.value = false;
    ingredientEditing.value = null;
    // Если указан начальный приход — создаём и проводим поставку
    const qty = Number(payload.initialQty || 0);
    if (!Number.isNaN(qty) && qty > 0 && created?.id && payload.warehouseId) {
      try {
        const supplyRes = await createSupply({
          warehouseId: Number(payload.warehouseId),
          supplierId: null,
          date: new Date().toISOString(),
          notes: 'Initial stock from Ingredient modal',
          items: [{ingredientId: Number(created.id), qty}],
        });
        const supplyId = supplyRes?.data?.id;
        if (supplyId) await postSupply(supplyId);
        toast.success('Начальный приход выполнен');
      } catch (e) {
        toast.error(e?.response?.data?.message || 'Не удалось выполнить начальный приход');
      }
    }
    if (selectedWarehouseId.value) await store.fetchWarehouseStock(selectedWarehouseId.value);
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Не удалось сохранить ингредиент');
  }
}

function unitShort(id) {
  const u = (units.value || []).find(x => x.id === id);
  return u ? (u.shortName || u.name) : '—';
}

const ingredientsForModal = computed(() => store.ingredients || []);

function openSupplyModal() {
  showSupply.value = true;
}

function closeSupplyModal() {
  showSupply.value = false;
}

async function saveSupply(payload) {
  try {
    const {warehouseId, date, supplierId, items, notes} = payload;
    const {data} = await createSupply({
      warehouseId: Number(warehouseId),
      supplierId: supplierId ? Number(supplierId) : null,
      date,
      notes: notes || null,
      items: items.map(i => ({
        ingredientId: Number(i.ingredientId),
        qty: Number(i.qty),
        expiresAt: i.expiresAt || null
      }))
    });
    const id = data?.id;
    if (id) await postSupply(id);
    showSupply.value = false;
    if (selectedWarehouseId.value) await store.fetchWarehouseStock(selectedWarehouseId.value);
  } catch (e) {
    console.error(e);
  }
}

// --- Логика таблицы остатков ---
// Фильтрация по поиску/дате
const filteredRows = computed(() => {
  const query = search.value.trim().toLowerCase();
  const rows = Array.isArray(warehouseStock.value) ? warehouseStock.value : [];
  return rows.filter(r => {
    const byQuery = !query || `${r.ingredientId ?? r.id ?? ''}`.toLowerCase().includes(query)
        || (r.name || '').toLowerCase().includes(query)
        || (r.supplierName || '').toLowerCase().includes(query)
        || (r.categoryName || '').toLowerCase().includes(query);
    const byDate = !dateFilter.value || (r.expiryDate === dateFilter.value);
    return byQuery && byDate;
  });
});

// Пагинация
const page = ref(1);
const perPage = ref(10);
const totalPages = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / perPage.value)));
const pagedRows = computed(() => {
  const start = (page.value - 1) * perPage.value;
  return filteredRows.value.slice(start, start + perPage.value);
});
watch([filteredRows, perPage], () => {
  page.value = 1;
});

function prevPage() {
  if (page.value > 1) page.value--;
}

function nextPage() {
  if (page.value < totalPages.value) page.value++;
}

// Подсветка строк
function rowClass(row) {
  const classes = [];
  const qty = Number(row?.quantity ?? NaN);
  if (!Number.isNaN(qty) && qty <= 2) classes.push('low-stock');
  try {
    if (row?.expiryDate) {
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const exp = new Date(row.expiryDate);
      exp.setHours(0, 0, 0, 0);
      if (exp.getTime() < today.getTime()) classes.push('expired');
    }
  } catch (_) {
  }
  return classes.join(' ');
}
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

.page-header h1 {
  margin: 0;
  font-size: 20px;
}

.page-controls {
  display: flex;
  gap: 8px;
  align-items: center;
}

.search {
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
}

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.warehouse-tabs-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
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

.items-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 6px;
}

.items-table th, .items-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.items-table thead th {
  font-weight: 600;
  color: #9ca3af;
}

.items-table tbody tr.row {
  border-top: 1px solid #eef2f7;
}

.items-table tbody tr.low-stock {
  background: #fff3cd;
}

.items-table tbody tr.expired {
  background: #f8d7da;
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
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.04);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.panel-actions {
  display: flex;
  gap: 8px;
}

.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
}

.pager {
  display: flex;
  gap: 8px;
  align-items: center;
}

.perpage select {
  margin-left: 8px;
  padding: 6px;
  border-radius: 6px;
}

.legend {
  display: flex;
  gap: 16px;
  margin-top: 10px;
  color: #6b7280;
  align-items: center;
}

.chip {
  display: inline-block;
  width: 18px;
  height: 12px;
  border-radius: 4px;
  margin-right: 8px;
}

.chip.low {
  background: #ffd964;
  border: 1px solid #d9a800;
}

.chip.expired {
  background: #ff6b6b;
  border: 1px solid #ff3b3b;
}
</style>
