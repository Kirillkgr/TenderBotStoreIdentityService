<template>
  <div class="modal-backdrop">
    <div class="modal">
      <header class="modal-header">
        <h3>Приход на склад</h3>
        <button class="close" @click="$emit('cancel')">✕</button>
      </header>
      <section class="modal-body">
        <div class="field">
          <label>Склад</label>
          <select v-model.number="warehouseId">
            <option :value="null" disabled>— выберите склад —</option>
            <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
          </select>
        </div>
        <div class="field">
          <label>Ингредиент</label>
          <select v-model.number="ingredientId">
            <option :value="null" disabled>— выберите ингредиент —</option>
            <option v-for="i in ingredients" :key="i.id" :value="i.id">{{ i.name }}</option>
          </select>
        </div>
        <div class="field">
          <label>Количество</label>
          <input v-model.number="qty" min="0" step="0.001" type="number"/>
        </div>
        <p v-if="error" class="error">{{ error }}</p>
      </section>
      <footer class="modal-footer">
        <button class="btn" @click="$emit('cancel')">Отмена</button>
        <button :disabled="!canSubmit || loading" class="btn primary" @click="onSubmit">
          <span v-if="loading">Сохранение…</span>
          <span v-else>Принять</span>
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import {computed, ref, watch} from 'vue';

const props = defineProps({
  ingredients: {type: Array, default: () => []},
  warehouses: {type: Array, default: () => []},
  presetWarehouseId: {type: Number, default: null},
  presetIngredientId: {type: Number, default: null},
});
const emit = defineEmits(['cancel', 'submit']);

const ingredientId = ref(props.presetIngredientId ?? null);
const warehouseId = ref(props.presetWarehouseId ?? null);
const qty = ref(0);
const loading = ref(false);
const error = ref('');

watch(() => props.presetWarehouseId, (v) => {
  if (v) warehouseId.value = v;
});
watch(() => props.presetIngredientId, (v) => {
  if (v) ingredientId.value = v;
});

const canSubmit = computed(() => {
  return !!warehouseId.value && !!ingredientId.value && typeof qty.value === 'number' && qty.value > 0;
});

async function onSubmit() {
  error.value = '';
  if (!canSubmit.value) {
    error.value = 'Укажите склад, ингредиент и количество > 0';
    return;
  }
  try {
    loading.value = true;
    await emit('submit', {
      ingredientId: Number(ingredientId.value),
      warehouseId: Number(warehouseId.value),
      qty: Number(qty.value)
    });
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || 'Не удалось выполнить приход';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .4);
  display: grid;
  place-items: center;
}

.modal {
  background: #fff;
  color: #111827;
  width: 520px;
  max-width: calc(100% - 24px);
  border-radius: 8px;
  overflow: hidden;
}

.modal-header, .modal-footer {
  padding: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.modal-footer {
  border-top: 1px solid #e5e7eb;
  border-bottom: 0;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.modal-body {
  padding: 12px;
}

.close {
  background: transparent;
  border: 0;
  font-size: 16px;
  cursor: pointer;
}

.field {
  display: grid;
  gap: 6px;
  margin-bottom: 10px;
}

.error {
  color: #b91c1c;
  margin: 6px 0 0;
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

select, input[type="number"] {
  padding: 6px 8px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
}
</style>
