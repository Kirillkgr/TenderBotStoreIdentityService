<template>
  <section class="inventory-page">
    <header class="page-header">
      <h1>Ингредиенты</h1>
      <div class="actions">
        <router-link :to="{ name: 'Suppliers' }" class="btn">Поставщики</router-link>
        <router-link :to="{ name: 'Units' }" class="btn">Единицы</router-link>
        <input v-model="q" placeholder="Поиск по названию" type="text"/>
        <select v-model.number="unitFilter">
          <option :value="0">Все единицы</option>
          <option v-for="u in units" :key="u.id" :value="u.id">{{ u.name }}</option>
        </select>
        <button
            v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
            class="btn primary"
            @click="onCreate"
        >Добавить ингредиент
        </button>
      </div>
    </header>

    <div v-if="loading" class="loading">Загрузка…</div>
    <div v-else>
      <table class="table">
        <thead>
        <tr>
          <th>#</th>
          <th>Название</th>
          <th>Ед.</th>
          <th>Упаковка</th>
          <th style="width:1%">Действия</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(x, idx) in filtered" :key="x.id">
          <td>{{ idx + 1 }}</td>
          <td>{{ x.name }}</td>
          <td>{{ unitName(x.unitId) }}</td>
          <td>{{ x.packageSize ?? '—' }}</td>
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

    <IngredientForm
        v-if="showForm"
        :ingredient="editing"
        :units="units"
        @cancel="onCancel"
        @save="onSave"
    />
  </section>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useInventoryStore} from '../../store/inventoryStore';
import IngredientForm from '../../components/inventory/IngredientForm.vue';

const store = useInventoryStore();
const loading = computed(() => store.ingredientsLoading || store.unitsLoading);
const error = computed(() => store.ingredientsError);
const items = computed(() => store.ingredients || []);
const units = computed(() => store.units || []);

const q = ref('');
const unitFilter = ref(0);

const filtered = computed(() => {
  const query = q.value.trim().toLowerCase();
  return (items.value || [])
      .filter(x => !unitFilter.value || x.unitId === unitFilter.value)
      .filter(x => !query || (x.name || '').toLowerCase().includes(query));
});

const showForm = ref(false);
const editing = ref(null);

function unitName(id) {
  const u = (units.value || []).find(u => u.id === id);
  return u ? (u.shortName || u.name) : '—';
}

async function load() {
  try {
    await Promise.all([
      store.fetchUnits(),
      store.fetchIngredients(),
    ]);
  } catch (e) {
    // noop; ошибки показываются через стор/алерты ниже
  }
}

function onCreate() {
  editing.value = null;
  showForm.value = true;
}

function onEdit(x) {
  editing.value = {...x};
  showForm.value = true;
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
    if (payload.id) {
      await store.updateIngredient(payload.id, {
        name: payload.name,
        unitId: payload.unitId,
        packageSize: payload.packageSize ?? null,
        notes: payload.notes || null,
      });
    } else {
      await store.createIngredient({
        name: payload.name,
        unitId: payload.unitId,
        packageSize: payload.packageSize ?? null,
        notes: payload.notes || null,
      });
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
</style>
