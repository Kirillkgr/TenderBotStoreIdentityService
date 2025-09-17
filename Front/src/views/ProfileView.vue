<template>
  <div class="profile-container">
    <div v-if="authStore.user" class="user-info">
      <p><strong>Пользователь:</strong> {{ authStore.user.lastName }} {{ authStore.user.firstName }} {{ authStore.user.patronymic }}</p>
      <p><strong>Логин:</strong> {{ authStore.user.username }}</p>
      <p><strong>Дата рождения:</strong> {{ formattedDateOfBirth }}</p>
    </div>
    <div v-else>
      <p>Загрузка данных...</p>
    </div>

    <hr />

    <div class="order-history">
      <h2>История заказов</h2>
      <div v-if="orderStore.loading">Загрузка заказов...</div>
      <div v-else-if="orderStore.orders.length === 0">У вас еще нет заказов.</div>
      <div v-else>
        <OrderListItem 
          v-for="order in orderStore.orders" 
          :key="order.id" 
          :order="order" 
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, computed } from 'vue';
import { useAuthStore } from '../store/auth';
import { useOrderStore } from '../store/order';
import OrderListItem from '../components/OrderListItem.vue';

const authStore = useAuthStore();
const orderStore = useOrderStore();

const formattedDateOfBirth = computed(() => {
  if (!authStore.user || !authStore.user.dateOfBirth) return '';
  const [year, month, day] = authStore.user.dateOfBirth.split('-');
  return `${day}.${month}`;
});

onMounted(() => {
  orderStore.fetchOrders();
});
</script>

<style scoped>
.profile-container {
  max-width: 800px;
  margin: auto;
  padding: 2rem;
  text-align: left; /* Улучшение читаемости */
}

.user-info {
  margin-bottom: 2rem;
}

hr {
  margin: 2rem 0;
  border: 0;
  border-top: 1px solid #4a627a; /* Стилизация под темную тему */
}

.order-history h2 {
  margin-bottom: 1.5rem;
}

/* Адаптация для мобильных устройств */
@media (max-width: 768px) {
  .profile-container {
    max-width: 100%;
    padding: 1rem;
  }

  h1 {
    font-size: 2.2em; /* Уменьшаем заголовок на мобильных */
  }
}
</style>
