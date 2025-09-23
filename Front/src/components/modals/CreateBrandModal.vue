<template>
  <Modal :is-modal-visible="true" @close="$emit('close')">
    <template #header>
      <h3>Создать новый бренд</h3>
    </template>
    <template #content>
      <Form @submit="onSubmit" :validation-schema="schema" class="form-body">
        <div class="form-group">
          <label for="brandName">Название бренда</label>
          <Field name="brandName" type="text" id="brandName" class="form-control" placeholder="Введите название бренда"/>
          <ErrorMessage name="brandName" class="error-message"/>
        </div>


                <div class="form-actions">
          <button type="button" class="btn btn-danger" @click="$emit('close')">Отмена</button>
          <button type="submit" class="btn btn-primary">Создать</button>
        </div>
      </Form>
    </template>
  </Modal>
</template>

<script setup>
import {ErrorMessage, Field, Form} from 'vee-validate';
import * as yup from 'yup';
import Modal from '@/components/Modal.vue';

const emit = defineEmits(['close', 'create-brand']);

const schema = yup.object({
  brandName: yup.string()
    .required('Название бренда обязательно')
    .min(2, 'Название должно содержать минимум 2 символа')
    .matches(/^[a-zA-Zа-яА-Я0-9\s-]+$/, 'Название содержит недопустимые символы')
    .test(
      'no-extra-spaces',
      'Название не должно состоять только из пробелов',
      value => value && value.trim().length >= 2
    ),
});

async function onSubmit(values, { resetForm }) {
  try {
    await emit('saved', values);
    resetForm();
    emit('close');
  } catch (error) {
    console.error('Ошибка при создании бренда:', error);
  }
}
</script>

<style scoped>
/* Стили идентичны CreateGroupModal.vue для консистентности */
.form-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  max-width: 380px;
  margin: 0 auto;
}

.form-group {
  text-align: left;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: var(--text-secondary);
}

.form-control {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 16px;
  background-color: #f7f7f7;
  transition: all 0.2s ease;
}

.form-control:focus {
  outline: none;
  border-color: var(--primary-color);
  background-color: #fff;
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.15);
}

.error-message {
  color: #ff3b30;
  font-size: 13px;
  padding-top: 4px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
}

.btn {
  padding: 12px 24px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease-in-out;
  border: none;
  text-align: center;
}

.btn-primary {
  background-color: var(--primary-color);
  color: white;
  box-shadow: 0 4px 15px rgba(0, 122, 255, 0.2);
}

.btn-primary:hover {
  background-color: var(--primary-color-dark);
  box-shadow: 0 6px 20px rgba(0, 122, 255, 0.3);
  transform: translateY(-2px);
}

.btn-secondary {
  background-color: #f0f2f5;
  color: var(--text-primary);
}

.btn-secondary:hover {
  background-color: #e4e6e9;
  transform: translateY(-2px);
}

.btn-danger {
  background-color: #f0f2f5;
  color: #ff3b30;
}

.btn-danger:hover {
  background-color: #ff3b30;
  color: white;
  transform: translateY(-2px);
}
</style>
