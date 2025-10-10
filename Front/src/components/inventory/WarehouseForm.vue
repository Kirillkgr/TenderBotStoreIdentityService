<template>
  <div class="modal-backdrop" @click.self="onCancel">
    <div class="modal">
      <h3>{{ model.id ? 'Изменить склад' : 'Новый склад' }}</h3>
      <form @submit.prevent="onSubmit">
        <label>
          Название
          <input v-model.trim="model.name" placeholder="Например, Главный склад" required type="text"/>
        </label>
        <div class="actions">
          <button class="btn primary" type="submit">Сохранить</button>
          <button class="btn" type="button" @click="onCancel">Отмена</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import {reactive, watch} from 'vue';

const props = defineProps({
  warehouse: {type: Object, default: null}
});
const emit = defineEmits(['save', 'cancel']);

const model = reactive({id: null, name: ''});

watch(() => props.warehouse, (val) => {
  model.id = val?.id ?? null;
  model.name = val?.name ?? '';
}, {immediate: true});

function onSubmit() {
  if (!model.name || !model.name.trim()) return;
  emit('save', {id: model.id, name: model.name.trim()});
}

function onCancel() {
  emit('cancel');
}
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: grid;
  place-items: center;
}

.modal {
  background: #fff;
  color: #111827;
  padding: 16px;
  border-radius: 8px;
  width: 420px;
  max-width: calc(100vw - 24px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

h3 {
  margin: 0 0 12px;
}

label {
  display: grid;
  gap: 6px;
  margin-bottom: 12px;
}

input[type="text"] {
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
}

.actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
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
</style>
