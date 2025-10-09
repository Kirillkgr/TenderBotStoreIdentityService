<template>
  <div class="order-item">
    <div class="order-header">
      <h4 class="order-title">
        Заказ #{{ order.id }}
        <span v-if="Number(order.rating||0) > 0" aria-hidden="true" class="stars stars--inline">
          <span v-for="n in 5" :key="n" :class="{ active: n <= Number(order.rating) }" class="star">★</span>
        </span>
      </h4>
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
        <button class="btn btn-chat" type="button" @click="$emit('open-message', order)">
          {{ isActive(order?.status) ? 'Написать администратору' : 'Просмотреть сообщения' }}
          <span v-if="nStore.hasUnreadByOrder(order.id)" class="btn-unread-dot" title="Есть новые сообщения"></span>
        </button>
        <button
            v-if="isCompleted(order?.status) && !hasReview(order)"
            class="btn btn-sm"
            style="margin-left: 8px;"
            type="button"
            @click.stop.prevent="$emit('open-review', order)">
          Оценить заказ
        </button>
        <button
            v-else-if="isCompleted(order?.status) && hasReview(order)"
            class="btn btn-sm"
            style="margin-left: 8px;"
            type="button"
            @click.stop.prevent="$emit('open-review-text', order)">
          Посмотреть отзыв
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {defineProps} from 'vue';
import {formatLocalDateTime} from '@/utils/datetime';
import {useNotificationsStore} from '@/store/notifications';

const emit = defineEmits(['open-message', 'open-review', 'open-review-text']);

const nStore = useNotificationsStore();

defineProps({
  order: {
    type: Object,
    required: true,
  },
});

function isActive(status) {
  const s = String(status || '').toUpperCase();
  const active = new Set(['QUEUED', 'PREPARING', 'READY_FOR_PICKUP', 'OUT_FOR_DELIVERY', 'DELIVERED']);
  return active.has(s);
}

function isCompleted(status) {
  const s = String(status || '').toUpperCase();
  return s === 'COMPLETED';
}

function reviewed(orderId) {
  try {
    return localStorage.getItem('reviewed_' + orderId) === '1';
  } catch (_) {
    return false;
  }
}

function hasReview(order) {
  if (!order) return false;
  const rated = Number.isFinite(Number(order.rating)) && Number(order.rating) > 0;
  return rated || reviewed(order.id);
}

function truncate(text, max) {
  try {
    const s = String(text || '');
    if (s.length <= max) return s;
    return s.slice(0, max).trimEnd() + '…';
  } catch (_) {
    return String(text ?? '');
  }
}

function formatPrice(val) {
  const n = Number(val);
  if (!Number.isFinite(n)) return '—';
  return new Intl.NumberFormat('ru-RU', {minimumFractionDigits: 2, maximumFractionDigits: 2}).format(n);
}

function formatDate(iso) {
  try {
    return formatLocalDateTime(iso);
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

.btn:disabled {
  opacity: .6;
  cursor: default;
}

.stars .star {
  color: #888;
}

.stars .star.active {
  color: #ffd54f;
  text-shadow: 0 0 4px rgba(255, 213, 79, .5);
}

.stars--inline {
  margin-left: 8px;
  font-size: .9em;
  vertical-align: middle;
}

.btn-chat {
  position: relative;
}

.btn-unread-dot {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #e53935;
  box-shadow: 0 0 0 2px rgba(36, 36, 36, 0.9);
}
</style>
