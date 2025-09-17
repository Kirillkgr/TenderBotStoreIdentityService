<template>
  <div class="order-item">
    <div class="order-header">
      <h4>Заказ #{{ order.id }}</h4>
      <span>Статус: {{ order.status }}</span>
    </div>
    <div class="order-body">
      <p><strong>Дата:</strong> {{ new Date(order.date).toLocaleDateString() }}</p>
      <p><strong>Сумма:</strong> {{ order.total }} ₽</p>
      <h5>Товары:</h5>
      <ul>
        <li v-for="item in order.items" :key="item.productId">
          {{ item.name }} - {{ item.quantity }} шт. x {{ item.unitPrice }} ₽
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { defineProps } from 'vue';

defineProps({
  order: {
    type: Object,
    required: true,
  },
});
</script>

<style scoped>
.order-item {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
  padding-bottom: 1rem;
  margin-bottom: 1rem;
}

.order-header h4 {
  margin: 0;
}

ul {
  list-style-type: none;
  padding-left: 0;
}

li {
  margin-bottom: 0.5rem;
}
</style>
