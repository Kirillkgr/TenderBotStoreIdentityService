<template>
  <div class="order-item">
    <div class="order-header">
      <h4>Заказ #{{ order.id }}</h4>
      <span>Статус: {{ order.status }}</span>
    </div>
    <div class="order-body">
      <p><strong>Дата:</strong> {{ formatDate(order.createdAt) }}</p>
      <p><strong>Сумма:</strong> {{ formatPrice(order.total) }} ₽</p>
      <h5>Товары:</h5>
      <ul>
        <li v-for="item in (order.items || [])" :key="item.id || item.productId">
          {{ item.productName || ('Товар #' + (item.productId ?? '')) }} - {{ item.quantity }} шт. x
          {{ formatPrice(item.price) }} ₽
        </li>
      </ul>
      <div class="actions">
        <button class="btn" type="button" @click="$emit('open-message', order)">Написать администратору</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {defineProps} from 'vue';

const emit = defineEmits(['open-message']);

defineProps({
  order: {
    type: Object,
    required: true,
  },
});

function formatPrice(val) {
  const n = Number(val);
  if (!Number.isFinite(n)) return '—';
  return new Intl.NumberFormat('ru-RU', {minimumFractionDigits: 2, maximumFractionDigits: 2}).format(n);
}

function formatDate(iso) {
  try {
    const d = new Date(iso);
    return isNaN(d) ? '—' : d.toLocaleString('ru-RU');
  } catch {
    return '—';
  }
}
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

.actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.btn {
  border: 1px solid var(--border);
  background: var(--input-bg);
  color: var(--text);
  border-radius: 8px;
  padding: 6px 10px;
  cursor: pointer;
}
</style>
