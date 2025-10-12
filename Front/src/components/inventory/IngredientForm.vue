<template>
  <div class="modal-backdrop">
    <div class="modal">
      <header class="modal-header">
        <h3>{{ ingredient?.id ? 'Редактировать ингредиент' : 'Новый ингредиент' }}</h3>
      </header>
      <section class="modal-body">
        <form @submit.prevent="submit">
          <div class="form-row">
            <label>Название<span class="req">*</span></label>
            <input v-model.trim="model.name" maxlength="255" required type="text"/>
          </div>
          <div class="form-row">
            <label>Склад<span class="req">*</span></label>
            <select v-model.number="model.warehouseId" required>
              <option :value="0" disabled>Выберите склад</option>
              <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
            </select>
          </div>
          <div class="form-row">
            <label class="row-flex">
              <span>Единица<span class="req">*</span></span>
              <button class="link" type="button" @click="toggleCreateUnit">+ создать единицу</button>
            </label>
            <select v-model.number="model.unitId" required>
              <option :value="0" disabled>Выберите единицу</option>
              <option v-for="u in units" :key="u.id" :value="u.id">{{ u.name }} ({{ u.shortName || u.name }})</option>
            </select>
            <div v-if="creatingUnit" class="inline-unit">
              <input v-model.trim="newUnit.name" maxlength="64" placeholder="Название единицы"/>
              <input v-model.trim="newUnit.shortName" maxlength="16" placeholder="Сокр. обозначение"/>
              <button :disabled="!newUnit.name || !newUnit.shortName" class="btn" type="button"
                      @click="createUnitInline">Создать
              </button>
            </div>
          </div>

          <div class="form-row">
            <label class="row-flex">
              <span>Фасовка</span>
              <button class="link" type="button" @click="toggleCreatePackaging">+ создать фасовку</button>
            </label>
            <select v-model.number="model.packagingId">
              <option :value="0">Без фасовки</option>
              <option v-for="p in packagings" :key="p.id" :value="p.id">{{ p.name }}</option>
            </select>
            <div v-if="creatingPackaging" class="inline-unit">
              <input v-model.trim="newPackaging.name" maxlength="64" placeholder="Название фасовки"/>
              <input v-model.number="newPackaging.size" min="0" placeholder="Размер (число)" step="0.001"
                     type="number"/>
              <select v-model.number="newPackaging.unitId">
                <option :value="0" disabled>Единица</option>
                <option v-for="u in units" :key="u.id" :value="u.id">{{ u.shortName || u.name }}</option>
              </select>
              <button :disabled="!newPackaging.name || !newPackaging.size || !newPackaging.unitId" class="btn"
                      type="button"
                      @click="createPackagingInline">Создать
              </button>
            </div>
            <small v-if="creatingPackaging" class="hint">Подсказка: название × количество (в единицах). Пример: «Пакет»
              × 1 кг.</small>
          </div>
          <div class="form-row two">
            <div>
              <label>Размер упаковки</label>
              <input v-model.number="model.packageSize" min="0" placeholder="например, 1.000" step="0.001"
                     type="number"/>
            </div>
            <div>
              <label>Заметки</label>
              <input v-model.trim="model.notes" maxlength="1024" placeholder="опционально"/>
            </div>
          </div>

          <div class="form-row two">
            <div>
              <label>Начальное количество</label>
              <input v-model.number="model.initialQty" min="0" placeholder="0" step="0.001" type="number"/>
            </div>
          </div>
          <div class="form-row grid-4">
            <div>
              <label>Белки (г)</label>
              <input v-model.number="model.protein" min="0" placeholder="опционально" step="0.01" type="number"/>
            </div>
            <div>
              <label>Жиры (г)</label>
              <input v-model.number="model.fat" min="0" placeholder="опционально" step="0.01" type="number"/>
            </div>
            <div>
              <label>Углеводы (г)</label>
              <input v-model.number="model.carbs" min="0" placeholder="опционально" step="0.01" type="number"/>
            </div>
            <div>
              <label>Калорийность (ккал)</label>
              <input v-model.number="model.calories" min="0" placeholder="опционально" step="0.1" type="number"/>
            </div>
          </div>
        </form>
      </section>
      <footer class="modal-footer">
        <button class="btn" @click="$emit('cancel')">Отмена</button>
        <button
            :disabled="!model.name || !model.unitId || !model.warehouseId || saving"
            class="btn primary"
            @click="submit"
        >
          <span v-if="saving" class="spinner"></span>
          Сохранить
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import {reactive, ref, watch} from 'vue';
import {useToast} from 'vue-toastification';
import {createUnit} from '../../services/inventory/unitService';
import {useInventoryStore} from '../../store/inventoryStore';

const props = defineProps({
  ingredient: {type: Object, default: null},
  units: {type: Array, default: () => []},
  packagings: {type: Array, default: () => []},
  warehouses: {type: Array, default: () => []},
  selectedWarehouseId: {type: Number, default: 0},
});
const emit = defineEmits(['save', 'cancel']);
const toast = useToast();
const store = useInventoryStore();

const model = reactive({
  id: null,
  name: '',
  warehouseId: 0,
  unitId: 0,
  packageSize: null,
  packagingId: 0,
  initialQty: 0,
  calories: null,
  protein: null,
  fat: null,
  carbs: null,
  notes: '',
});
const saving = ref(false);

const creatingUnit = ref(false);
const newUnit = reactive({name: '', shortName: ''});
const creatingPackaging = ref(false);
const newPackaging = reactive({name: '', unitId: 0, size: null});

watch(() => props.ingredient, (val) => {
  model.id = val?.id || null;
  model.name = val?.name || '';
  model.warehouseId = props.selectedWarehouseId || 0;
  model.unitId = val?.unitId || 0;
  model.packageSize = val?.packageSize ?? null;
  model.notes = val?.notes || '';
}, {immediate: true});

function toggleCreateUnit() {
  creatingUnit.value = !creatingUnit.value;
}

function toggleCreatePackaging() {
  creatingPackaging.value = !creatingPackaging.value;
}

async function createPackagingInline() {
  try {
    const res = await store.createPackaging({
      name: newPackaging.name,
      unitId: newPackaging.unitId,
      size: Number(newPackaging.size)
    });
    newPackaging.name = '';
    newPackaging.unitId = 0;
    newPackaging.size = null;
    creatingPackaging.value = false;
    await store.fetchPackagings();
    // выбрать свежесозданную фасовку, если API вернул id через store — попробуем найти последнюю
    const last = (store.packagings || []).slice(-1)[0];
    if (last?.id) model.packagingId = last.id;
  } catch (e) {
    toast.error(e?.response?.data?.message || e?.message || 'Не удалось создать фасовку');
  }
}

async function createUnitInline() {
  try {
    if (!newUnit.name || !newUnit.shortName) return;
    const payload = {name: newUnit.name, shortName: newUnit.shortName};
    const res = await createUnit(payload);
    toast.success('Единица создана');
    newUnit.name = '';
    newUnit.shortName = '';
    creatingUnit.value = false;
    await store.fetchUnits();
    const createdId = res?.data?.id;
    if (createdId) model.unitId = createdId;
  } catch (e) {
    toast.error(e?.response?.data?.message || e?.message || 'Не удалось создать единицу');
  }
}

async function submit() {
  if (!model.name || !model.unitId || !model.warehouseId) return;
  try {
    saving.value = true;
    emit('save', {...model});
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(2, 6, 23, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal {
  background: #ffffff;
  color: #111827;
  border-radius: 8px;
  padding: 16px;
  min-width: 380px;
  max-width: 640px;
  width: 100%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}

.modal-header {
  margin-bottom: 8px;
}

.modal-body {
  padding: 8px 0;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 10px;
}

.form-row.two {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.row-flex {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

label {
  font-weight: 600;
}

.req {
  color: #ef4444;
  margin-left: 4px;
}

input, select, textarea {
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #ffffff;
  color: #111827;
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

.link {
  background: none;
  border: none;
  color: #2563eb;
  cursor: pointer;
  padding: 0;
}

.inline-unit {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-top: 6px;
}

.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid #fff;
  border-right-color: transparent;
  border-radius: 50%;
  margin-right: 6px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 640px) {
  .form-row.two {
    grid-template-columns: 1fr;
  }
}

/* Dark theme override */
@media (prefers-color-scheme: dark) {
  .modal {
    background: #1f2937;
    color: #e5e7eb;
  }

  input, select, textarea {
    background: #111827;
    color: #e5e7eb;
    border: 1px solid #374151;
  }

  .btn {
    background: #1f2937;
    color: #e5e7eb;
    border-color: #374151;
  }
}
</style>
