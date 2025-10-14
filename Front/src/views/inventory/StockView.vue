<template>
  <section class="stock-page">
    <header class="page-header">
      <h1>Остатки</h1>
      <div class="filters">
        <label>
          Склад
          <select v-model.number="warehouseId">
            <option :value="null">— любой —</option>
            <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
          </select>
        </label>
        <label>
          Ингредиент
          <select v-model.number="ingredientId">
            <option :value="null">— любой —</option>
            <option v-for="i in ingredients" :key="i.id" :value="i.id">{{ i.name }}</option>
          </select>
        </label>
        <button :disabled="!canQuery" class="btn" @click="reload">Показать</button>
      </div>
      <div class="actions">
        <button
            v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
            :disabled="!canMutate"
            class="btn"
            @click="openIncrease"
        >Приход
        </button>
        <button
            v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
            :disabled="!canMutate"
            class="btn danger"
            @click="openDecrease"
        >Списание
        </button>
      </div>
    </header>

    <div v-if="loading" class="loading">Загрузка…</div>
    <div v-else>
      <table class="items-table">
        <thead>
        <tr>
          <th>ID</th>
          <th>Название</th>
          <th>Ед.</th>
          <th>Упак.</th>
          <th>Кол-во</th>
          <th>Срок годн.</th>
          <th>Последняя поставка</th>
          <th>Действия</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(row, idx) in rows" :key="row.ingredientId || idx">
          <td class="muted">{{ row.ingredientId }}</td>
          <td>{{ row.name }}</td>
          <td>{{ row.unitName || '—' }}</td>
          <td>{{ row.packageSize ?? '—' }}</td>
          <td>{{ row.quantity }}</td>
          <td>{{ row.earliestExpiry || '—' }}</td>
          <td>{{ row.lastSupplyDate || '—' }}</td>
          <td class="row-actions">
            <button
                v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
                :disabled="!warehouseId"
                class="btn small"
                title="Приход"
                @click="openIncreaseFor(row)"
            >＋
            </button>
            <button
                v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
                :disabled="!warehouseId"
                class="btn small danger"
                title="Списание"
                @click="openDecreaseFor(row)"
            >−
            </button>
          </td>
        </tr>
        <tr v-if="rows.length === 0">
          <td class="muted" colspan="8">Нет данных. Задайте фильтр и нажмите «Показать».</td>
        </tr>
        </tbody>
      </table>
    </div>

    <StockIncreaseModal
        v-if="showIncrease"
        :ingredients="ingredients"
        :preset-ingredient-id="selectedIngredientId"
        :preset-warehouse-id="warehouseId"
        :warehouses="warehouses"
        @cancel="showIncrease=false"
        @submit="doIncrease"
    />

    <StockDecreaseModal
        v-if="showDecrease"
        :ingredients="ingredientsDecrease"
        :preset-ingredient-id="selectedIngredientId"
        :preset-warehouse-id="warehouseId"
        :warehouses="warehouses"
        @cancel="showDecrease=false"
        @submit="doDecrease"
    />
  </section>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useInventoryStore} from '../../store/inventoryStore';
import StockIncreaseModal from '../../components/inventory/StockIncreaseModal.vue';
import StockDecreaseModal from '../../components/inventory/StockDecreaseModal.vue';
import {useToast} from 'vue-toastification';

const store = useInventoryStore();
const toast = useToast();

const warehouses = computed(() => store.warehouses || []);
const ingredients = computed(() => store.ingredients || []);

const rows = computed(() => store.stockRows || []);
// Для списания разрешаем только те ингредиенты, что реально есть на складе (qty > 0)
const ingredientsDecrease = computed(() => {
  const list = Array.isArray(rows.value) ? rows.value : [];
  const allowedIds = new Set(list.filter(r => Number(r.quantity) > 0).map(r => r.ingredientId));
  const all = Array.isArray(ingredients.value) ? ingredients.value : [];
  const filtered = all.filter(i => allowedIds.has(i.id));
  // На случай рассинхронизации справочника ингредиентов — добавим плейсхолдеры по id из остатков
  const knownIds = new Set(filtered.map(i => i.id));
  for (const id of allowedIds) {
    if (!knownIds.has(id)) filtered.push({id, name: `#${id}`});
  }
  return filtered;
});
const loading = computed(() => store.stockLoading);

const warehouseId = ref(null);
const ingredientId = ref(null);
const selectedIngredientId = ref(null);

const canQuery = computed(() => !!warehouseId.value || !!ingredientId.value);
const canMutate = computed(() => !!warehouseId.value && !!ingredientId.value);

const showIncrease = ref(false);
const showDecrease = ref(false);

async function bootstrap() {
  try {
    await Promise.all([
      store.fetchWarehouses(),
      store.fetchIngredients(),
    ]);
  } catch (_) {
  }
}

function reload() {
  if (!canQuery.value) return;
  store.fetchStock({warehouseId: warehouseId.value || null, ingredientId: ingredientId.value || null})
      .catch(e => toast.error(e?.response?.data?.message || e?.message || 'Ошибка загрузки'));
}

function openIncrease() {
  // если пользователь выбрал ингредиент в фильтре — подставим его в модалку
  selectedIngredientId.value = ingredientId.value || null;
  showIncrease.value = true;
}

function openDecrease() {
  // списывать можно только имеющееся — но модалка получит уже отфильтрованный список
  selectedIngredientId.value = ingredientId.value || null;
  showDecrease.value = true;
}

function openIncreaseFor(row) {
  if (!row?.ingredientId || !warehouseId.value) return;
  selectedIngredientId.value = row.ingredientId;
  showIncrease.value = true;
}

function openDecreaseFor(row) {
  if (!row?.ingredientId || !warehouseId.value) return;
  if (!(Number(row.quantity) > 0)) return; // защита от нулевого остатка
  selectedIngredientId.value = row.ingredientId;
  showDecrease.value = true;
}

async function doIncrease(payload) {
  try {
    await store.increaseStock(payload);
    showIncrease.value = false;
    toast.success('Остаток увеличен');
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Не удалось выполнить приход');
  }
}

async function doDecrease(payload) {
  try {
    await store.decreaseStock(payload);
    showDecrease.value = false;
    toast.success('Списание выполнено');
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Не удалось выполнить списание');
  }
}

onMounted(bootstrap);
</script>

<style scoped>
.stock-page {
  padding: 16px;
}

.page-header {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: start;
  gap: 12px;
  margin-bottom: 12px;
}

.filters {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.actions {
  display: flex;
  gap: 8px;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
}

.items-table th, .items-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.muted {
  color: #6b7280;
}

.btn {
  padding: 6px 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}

.btn.danger {
  background: #ef4444;
  color: #fff;
  border-color: #ef4444;
}
</style>
