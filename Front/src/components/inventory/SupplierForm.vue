<template>
  <div class="modal-backdrop" @click.self="onCancel">
    <div class="modal">
      <h3>{{ model.id ? 'Изменить поставщика' : 'Новый поставщик' }}</h3>
      <form @submit.prevent="onSubmit">
        <label>
          Название
          <input v-model.trim="model.name" placeholder='Например, ООО "АзияФуд"' required type="text"/>
        </label>
        <div class="row">
          <label>
            Телефон
            <input v-model.trim="model.phone" placeholder="+7 999 000 00 00" type="text"/>
          </label>
          <label>
            Email
            <input v-model.trim="model.email" placeholder="mail@example.com" type="email"/>
          </label>
        </div>
        <label>
          Адрес
          <input v-model.trim="model.address" placeholder="Город, улица, дом" type="text"/>
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
  supplier: {type: Object, default: null}
});
const emit = defineEmits(['save', 'cancel']);

const model = reactive({id: null, name: '', phone: '', email: '', address: ''});

watch(() => props.supplier, (val) => {
  model.id = val?.id ?? null;
  model.name = val?.name ?? '';
  model.phone = val?.phone ?? '';
  model.email = val?.email ?? '';
  model.address = val?.address ?? '';
}, {immediate: true});

function onSubmit() {
  if (!model.name || !model.name.trim()) return;
  emit('save', {
    id: model.id,
    name: model.name.trim(),
    phone: model.phone?.trim() || null,
    email: model.email?.trim() || null,
    address: model.address?.trim() || null,
  });
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
  z-index: 40;
}

.modal {
  background: #0f1720;
  color: #e6eef8;
  padding: 16px;
  border-radius: 10px;
  width: 520px;
  max-width: calc(100vw - 24px);
  border: 1px solid #1f2933;
}

h3 {
  margin: 0 0 12px;
}

label {
  display: grid;
  gap: 6px;
  margin-bottom: 12px;
}

.row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

input[type="text"], input[type="email"] {
  padding: 10px;
  border: 1px solid #1f2933;
  border-radius: 8px;
  background: #0b0b0d;
  color: #e6eef8;
}

.actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.btn {
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
}

.btn.primary {
  background: #1f6feb;
  color: #fff;
  border-color: #1f6feb;
}

@media (max-width: 560px) {
  .row {
    grid-template-columns: 1fr;
  }
}
</style>
