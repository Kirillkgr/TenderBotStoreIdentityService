<template>
  <div class="checkout-container">
    <h1>Оформление заказа</h1>
    <Form @submit="handlePlaceOrder" :validation-schema="schema">
      <div class="form-group">
        <label for="address">Адрес доставки</label>
        <Field name="address" type="text" id="address" />
        <ErrorMessage name="address" class="error-message" />
      </div>

      <div class="form-group">
        <label for="phone">Контактный телефон</label>
        <Field name="phone" type="text" id="phone" />
        <ErrorMessage name="phone" class="error-message" />
      </div>

      <div class="form-group">
        <label for="comment">Комментарий к заказу</label>
        <Field name="comment" as="textarea" id="comment" />
      </div>

      <button type="submit" :disabled="isSubmitting">Оформить</button>
    </Form>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { Form, Field, ErrorMessage } from 'vee-validate';
import * as yup from 'yup';
import { useRouter } from 'vue-router';
import * as orderService from '../services/orderService';
import { useToast } from 'vue-toastification';

const router = useRouter();
const isSubmitting = ref(false);
const toast = useToast();

const schema = yup.object({
  address: yup.string().required('Адрес обязателен для заполнения'),
  phone: yup.string().required('Телефон обязателен').matches(/^[\d\s()+-]+$/, 'Некорректный формат телефона'),
  comment: yup.string(),
});

async function handlePlaceOrder(values) {
  isSubmitting.value = true;
  try {
    await orderService.placeOrder(values);
    toast.success('Ваш заказ успешно оформлен!');
    // Опционально: очистить корзину после заказа
    // const cartStore = useCartStore();
    // cartStore.clearCart();
    router.push('/profile'); // Перенаправляем в профиль, где будут заказы
  } catch (error) {
    toast.error('Произошла ошибка при оформлении заказа.');
    console.error(error);
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<style scoped>
/* Mobile-first styles */
.checkout-container {
  max-width: 600px;
  margin: auto;
  padding: 1rem; /* Уменьшаем отступы для мобильных */
}

/* Tablet and Desktop styles */
@media (min-width: 768px) {
  .checkout-container {
    padding: 2rem; /* Возвращаем исходные отступы */
  }
}

.form-group {
  margin-bottom: 1.5rem;
}

label {
  display: block;
  margin-bottom: 0.5rem;
}

input, textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.error-message {
  color: #ff4d4d;
  margin-top: 0.25rem;
}

button {
  width: 100%;
  padding: 1rem;
  background-color: #42b983;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1.1rem;
  cursor: pointer;
}

button:disabled {
  background-color: #ccc;
}
</style>
