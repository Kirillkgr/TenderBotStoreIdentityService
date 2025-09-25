<template>
  <div class="my-orders">
    <h1>Мои заказы</h1>

    <div v-if="loading" class="muted">Загрузка заказов...</div>
    <div v-else-if="orders.length === 0" class="muted">Заказов пока нет</div>

    <div v-else class="orders">
      <div v-for="o in orders" :key="o.id" class="card">
        <div class="row top">
          <div class="left">
            <div class="id">Заказ #{{ o.id }}</div>
            <div class="meta">
              <span class="status">Статус: <b>{{ o.status }}</b></span>
              <span v-if="o.deliveryMode" class="mode"> · {{ o.deliveryMode }}</span>
            </div>
          </div>
          <div class="right">
            <div class="date">{{ formatDate(o.createdAt) }}</div>
            <div class="total">Итого: <b>{{ formatPrice(o.total) }} ₽</b></div>
          </div>
        </div>
        <div v-if="Array.isArray(o.items)" class="items">
          <div v-for="it in o.items" :key="it.id" class="item">
            <span class="name">{{ it.productName || ('Товар #' + (it.productId ?? '')) }}</span>
            <span class="qty">× {{ it.quantity }}</span>
            <span class="price">{{ formatPrice(it.price) }} ₽</span>
          </div>
        </div>
        <div class="actions">
          <button
              v-if="isActive(o.status)"
              class="btn"
              @click="openMessage(o)">
            Написать администратору
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- Общая модалка чата -->
  <ChatModal v-if="messageModal.visible" :order="messageModal.order" role="client" @close="closeMessage"/>
</template>

<script setup>
import {onMounted, onBeforeUnmount, ref} from 'vue';
import {useOrderStore} from '@/store/order';
import {getNotificationsClient} from '@/services/notifications';
import ChatModal from '@/components/ChatModal.vue';

const loading = ref(false);
const orders = ref([]);
const orderStore = useOrderStore();
const messageModal = ref({visible: false, order: null});

function formatPrice(val) {
  try {
    const num = Number(val);
    if (!Number.isFinite(num)) return String(val);
    return new Intl.NumberFormat('ru-RU', {minimumFractionDigits: 2, maximumFractionDigits: 2}).format(num);
  } catch (_) {
    return String(val ?? '');
  }
}

function formatDate(iso) {
  try {
    const d = new Date(iso);
    if (isNaN(d)) return String(iso ?? '');
    return d.toLocaleString('ru-RU');
  } catch (_) {
    return String(iso ?? '');
  }
}

function isActive(status) {
  return status !== 'COMPLETED' && status !== 'CANCELED';
}

function openMessage(order) {
  messageModal.value = {visible: true, order};
}

function closeMessage() {
  messageModal.value.visible = false;
}

let unsubscribe = null;

onMounted(async () => {
  loading.value = true;
  try {
    await orderStore.fetchOrders();
    orders.value = orderStore.orders;
  } catch (e) {
    console.error('Не удалось загрузить заказы', e);
    orders.value = [];
  } finally {
    loading.value = false;
  }

  const client = getNotificationsClient();
  client.start();
  unsubscribe = client.subscribe((evt) => {
    try {
      if (evt?.type === 'COURIER_MESSAGE' && evt.orderId) {
        const order = (orderStore.orders || []).find(o => o.id === evt.orderId);
        if (order && isActive(order.status)) openMessage(order);
      } else if (evt?.type === 'ORDER_STATUS_CHANGED' && evt.orderId) {
        orderStore.fetchOrders();
      }
    } catch (_) {
    }
  });
});

onBeforeUnmount(() => {
  if (typeof unsubscribe === 'function') unsubscribe();
});
</script>

<style scoped>
.my-orders {
  max-width: 820px;
  margin: 0 auto;
  text-align: left;
  padding: 12px;
}

.muted {
  color: #999;
}

.orders {
  display: grid;
  gap: 12px;
}

.card {
  border: 1px solid #3a3a3a;
  border-radius: 10px;
  padding: 12px;
  background: #2b2b2b;
}

.row.top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
}

.id {
  font-weight: 700;
  font-size: 1.1rem;
}

.meta {
  color: #bbb;
  font-size: .9rem;
}

.right {
  text-align: right;
  color: #bbb;
}

.items {
  border-top: 1px dashed #444;
  margin-top: 8px;
  padding-top: 8px;
  display: grid;
  gap: 6px;
}

.item {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 8px;
  align-items: center;
}

.item .qty {
  color: #bbb;
}

.item .price {
  font-weight: 600;
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

.input {
  width: 100%;
  border: 1px solid var(--border);
  background: var(--input-bg);
  color: var(--text);
  border-radius: 8px;
  padding: 6px 10px;
}

.error {
  color: var(--danger);
}

.modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal__dialog {
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px;
  width: min(600px, 96vw);
}

.modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal__body {
  margin: 8px 0;
}

.modal__footer {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
