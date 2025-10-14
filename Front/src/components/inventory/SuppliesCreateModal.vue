<template>
  <transition name="fade-scale">
    <div v-if="visible" class="modal-overlay" @click.self="onClose">
      <div class="modal-card">
        <div class="modal-head">
          <h3>Новая поставка</h3>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <label>Склад</label>
            <select v-model="form.warehouseId" @change="onWarehouseChange">
              <option :value="''" disabled>Выберите склад</option>
              <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
            </select>
          </div>
          <div class="form-row">
            <label>Поставщик (необязательно)</label>
            <select v-model="form.supplierId" :disabled="suppliersLoading">
              <option :value="''">— Не указан —</option>
              <option v-for="s in suppliers" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>
          <div class="form-row grid-2">
            <div>
              <label>Дата поставки</label>
              <input v-model="form.dateLocal" type="datetime-local"/>
            </div>
            <div>
              <label>Заметки</label>
              <input v-model="form.notes" placeholder="Комментарий" type="text"/>
            </div>
          </div>

          <div class="items-block">
            <div class="items-head">
              <strong>Позиции</strong>
              <button :disabled="!form.warehouseId" class="btn small" type="button" @click="addItem">Добавить позицию
              </button>
            </div>
            <div v-if="!form.items.length" class="hint">Добавьте хотя бы одну позицию</div>
            <div v-for="(it, idx) in form.items" :key="idx" class="item-row">
              <select v-model="it.ingredientId" :disabled="!form.warehouseId || loadingIngredients">
                <option :value="''" disabled>
                  {{ !form.warehouseId ? 'Сначала выберите склад' : (loadingIngredients ? 'Загрузка…' : 'Ингредиент') }}
                </option>
                <option v-for="i in warehouseIngredients" :key="i.id" :value="i.id">
                  {{ i.name }} — {{ i.qty ?? 0 }}{{ i.unit ? ' ' + i.unit : '' }}
                </option>
              </select>
              <input v-model.number="it.qty" min="0" placeholder="Кол-во" step="0.001" type="number"/>
              <input v-model="it.expiresAt" type="date"/>
              <button class="btn small danger" type="button" @click="removeItem(idx)">Удалить</button>
            </div>
          </div>

          <p v-if="error" class="error">{{ error }}</p>
        </div>
        <div class="modal-foot">
          <button class="btn" type="button" @click="onClose">Отмена</button>
          <button :disabled="submitting" class="btn btn-primary" type="button" @click="onSubmit">Создать</button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import {onMounted, ref, watch} from 'vue';
import {getWarehouses} from '../../services/inventory/warehouseService';
import {listStock} from '../../services/inventory/stockService';
import {createSupply, postSupply} from '../../services/inventory/suppliesService';
import {getSuppliers} from '../../services/inventory/supplierService';

const props = defineProps({visible: {type: Boolean, default: false}});
const emit = defineEmits(['close', 'created']);

const warehouses = ref([]);
const warehouseIngredients = ref([]);
const loadingIngredients = ref(false);
const submitting = ref(false);
const error = ref('');
const suppliers = ref([]);
const suppliersLoading = ref(false);

const form = ref({
  warehouseId: '',
  supplierId: '',
  dateLocal: '',
  notes: '',
  items: [], // { ingredientId:'', qty:0, expiresAt:'' }
});

function resetForm() {
  form.value = {warehouseId: '', supplierId: '', dateLocal: defaultDateLocal(), notes: '', items: []};
  warehouseIngredients.value = [];
  error.value = '';
}

function onClose() {
  emit('close');
}

async function loadWarehouses() {
  try {
    const {data} = await getWarehouses();
    warehouses.value = Array.isArray(data?.items) ? data.items : (Array.isArray(data) ? data : []);
  } catch (e) {
    console.error('load warehouses', e);
  }
}

async function loadSuppliers() {
  suppliersLoading.value = true;
  try {
    const {data} = await getSuppliers();
    suppliers.value = Array.isArray(data?.items) ? data.items : (Array.isArray(data) ? data : []);
  } catch (e) {
    console.error('load suppliers', e);
  } finally {
    suppliersLoading.value = false;
  }
}

async function onWarehouseChange() {
  // очистим позиции при смене склада и оставим одну пустую строку
  form.value.items = [];
  addItem();
  if (!form.value.warehouseId) {
    warehouseIngredients.value = [];
    return;
  }
  loadingIngredients.value = true;
  try {
    // Берём существующие остатки на складе и предлагаем их ингредиенты
    const {data} = await listStock({warehouseId: form.value.warehouseId});
    const list = Array.isArray(data?.items) ? data.items : (Array.isArray(data) ? data : []);
    const map = new Map();
    list.forEach(s => {
      const id = s.ingredientId || s.ingredient?.id;
      const name = s.ingredientName || s.name || s.ingredient?.name || (id ? `Ингредиент #${id}` : 'Ингредиент');
      const qty = s.quantity ?? s.qty ?? 0;
      const unit = s.unitName || (s.unit?.name) || '';
      if (id) map.set(id, {id, name, qty, unit});
    });
    warehouseIngredients.value = Array.from(map.values());
  } catch (e) {
    console.error('load warehouse ingredients', e);
    warehouseIngredients.value = [];
  } finally {
    loadingIngredients.value = false;
  }
}

function addItem() {
  form.value.items.push({ingredientId: '', qty: 0, expiresAt: ''});
}

function removeItem(idx) {
  if (idx >= 0 && idx < form.value.items.length) {
    form.value.items.splice(idx, 1);
  }
}

async function onSubmit() {
  error.value = '';
  if (!form.value.warehouseId) {
    error.value = 'Выберите склад';
    return;
  }
  if (!form.value.items.length) {
    error.value = 'Добавьте хотя бы одну позицию';
    return;
  }
  for (const it of form.value.items) {
    if (!it.ingredientId) {
      error.value = 'Укажите ингредиент для каждой позиции';
      return;
    }
    if (!it.qty || it.qty <= 0) {
      error.value = 'Количество должно быть > 0';
      return;
    }
  }
  submitting.value = true;
  try {
    const payload = {
      warehouseId: Number(form.value.warehouseId),
      supplierId: form.value.supplierId ? Number(form.value.supplierId) : null,
      date: form.value.dateLocal ? new Date(form.value.dateLocal).toISOString() : new Date().toISOString(),
      notes: form.value.notes || null,
      items: form.value.items.map(x => ({
        ingredientId: Number(x.ingredientId),
        qty: Number(x.qty),
        expiresAt: x.expiresAt || null,
      })),
    };
    const {data} = await createSupply(payload);
    const id = data?.id;
    if (id) {
      try {
        await postSupply(id);
      } catch (_) {
      }
    }
    emit('created');
    resetForm();
  } catch (e) {
    console.error('create supply', e);
    error.value = e?.response?.data?.message || 'Не удалось создать поставку';
  } finally {
    submitting.value = false;
  }
}

function defaultDateLocal() {
  try {
    const d = new Date();
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 16);
  } catch {
    return '';
  }
}

watch(() => props.visible, (v) => {
  if (v) {
    resetForm();
    if (!warehouses.value.length) loadWarehouses();
    if (!suppliers.value.length) loadSuppliers();
  }
});
onMounted(() => {
  loadWarehouses();
  loadSuppliers();
});
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 5000;
}

.modal-card {
  width: min(560px, 92vw);
  background: #1f1f1f;
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 12px;
  overflow: hidden;
}

.modal-head {
  padding: 12px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, .08);
}

.modal-body {
  padding: 14px;
  display: grid;
  gap: 12px;
}

.modal-foot {
  padding: 12px 14px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid rgba(255, 255, 255, .08);
}

.form-row {
  display: grid;
  gap: 6px;
}

.grid-2 {
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

label {
  font-size: 13px;
  color: #cfd8dc;
}

select, input[type=number] {
  width: 100%;
  background: rgba(255, 255, 255, .06);
  color: var(--text);
  border: 1px solid rgba(255, 255, 255, .12);
  border-radius: 8px;
  padding: 8px 10px;
}

input[type="text"], input[type="date"], input[type="datetime-local"] {
  width: 100%;
  background: rgba(255, 255, 255, .06);
  color: var(--text);
  border: 1px solid rgba(255, 255, 255, .12);
  border-radius: 8px;
  padding: 8px 10px;
}

.hint {
  color: #bdbdbd;
  font-size: 12px;
}

.error {
  color: #ff6b6b;
}

.btn {
  border: 1px solid rgba(255, 255, 255, .2);
  background: rgba(255, 255, 255, .08);
  color: var(--text);
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
}

.btn:hover {
  background: rgba(255, 255, 255, .14);
}

.btn-primary {
  background: #4a6cf7;
  border-color: #4a6cf7;
  color: #fff;
}

.btn.small {
  padding: 6px 8px;
  font-size: 12px;
}

.btn.danger {
  background: #ef4444;
  border-color: #ef4444;
  color: #fff;
}

.items-block {
  display: grid;
  gap: 8px;
}

.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-row {
  display: grid;
  grid-template-columns: 2.2fr 1fr 1.4fr auto;
  gap: 8px;
  align-items: center;
}

.fade-scale-enter-active, .fade-scale-leave-active {
  transition: opacity .18s ease, transform .18s ease;
}

.fade-scale-enter-from, .fade-scale-leave-to {
  opacity: 0;
  transform: scale(.98);
}
</style>
