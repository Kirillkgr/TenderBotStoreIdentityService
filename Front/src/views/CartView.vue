<template>
  <div class="cart-container">
    <h1>Ваша корзина</h1>
    <div v-if="cartStore.loading">Загрузка...</div>
    <div v-else-if="cartStore.items.length === 0">Ваша корзина пуста.</div>
    <div v-else>
      <div class="cart-items">
        <div v-for="item in cartStore.items" :key="item.cartItemId" class="cart-item">
          <div class="item-info">
            <span>{{ item.name }}</span>
            <span>{{ item.quantity }} x {{ item.unitPrice }} ₽</span>
          </div>
          <div class="item-total">
            <span>{{ item.totalPrice }} ₽</span>
            <button @click="handleRemoveItem(item.cartItemId)" class="remove-btn">Удалить</button>
          </div>
        </div>
      </div>
      <div class="cart-summary">
        <h3>Итого: {{ cartStore.total }} ₽</h3>
        <router-link to="/checkout" class="checkout-btn">Оформить заказ</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useCartStore } from '../store/cart';
import { useToast } from 'vue-toastification';

const cartStore = useCartStore();
const toast = useToast();

onMounted(() => {
  cartStore.fetchCart();
});

async function handleRemoveItem(cartItemId) {
  try {
    await cartStore.removeItem(cartItemId);
    toast.success('Товар удален из корзины.');
  } catch (error) {
    toast.error('Не удалось удалить товар.');
  }
}
</script>

<style scoped>
/* Mobile-first styles */
.cart-container {
  max-width: 800px;
  margin: auto;
  padding: 1rem; /* Меньше отступы */
}

.cart-items {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 2rem;
}

.cart-item {
  display: flex;
  flex-direction: column; /* Вертикальное расположение на мобильных */
  align-items: flex-start; /* Выравнивание по левому краю */
  gap: 1rem; /* Пространство между блоками */
  padding: 1rem;
  border: 1px solid #4a627a;
  border-radius: 8px;
  background-color: #2c3e50;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.item-total {
  display: flex;
  align-items: center;
  justify-content: space-between; /* Распределяем элементы */
  width: 100%; /* Занимает всю ширину */
  gap: 1rem;
}

.remove-btn {
  background-color: #e74c3c;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.remove-btn:hover {
  background-color: #c0392b;
}

.cart-summary {
  text-align: center; /* Центрируем на мобильных */
  margin-top: 1rem;
}

.checkout-btn {
  display: block; /* Блочный элемент на всю ширину */
  width: 100%;
  padding: 0.75rem 1.5rem;
  background-color: #42b983;
  color: white;
  text-decoration: none;
  border-radius: 4px;
  cursor: pointer;
  text-align: center;
}

.checkout-btn:hover {
  background-color: #369f72;
}

/* Tablet and Desktop styles */
@media (min-width: 768px) {
  .cart-container {
    padding: 2rem;
  }

  .cart-item {
    flex-direction: row; /* Возвращаем горизонтальное расположение */
    justify-content: space-between;
    align-items: center;
  }

  .item-total {
    justify-content: flex-end; /* Выравниваем по правому краю */
    width: auto; /* Авто-ширина */
  }

  .cart-summary {
    text-align: right; /* Возвращаем выравнивание по правому краю */
  }

  .checkout-btn {
    display: inline-block; /* Возвращаем строчно-блочный тип */
    width: auto;
  }
}
</style>
