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
              class="btn btn-chat"
              @click="openMessage(o)">
            {{ isActive(o.status) ? 'Написать администратору' : 'Просмотреть сообщения' }}
            <span v-if="nStore.hasUnreadByOrder(o.id)" class="btn-unread-dot" title="Есть новые сообщения"></span>
          </button>
          <button
              v-if="isCompleted(o.status) && !hasReview(o)"
              class="btn btn-sm"
              style="margin-left: 8px;"
              type="button"
              @click="openReview(o)">
            Оценить заказ
          </button>
          <div v-if="o.rating" class="rating-wrap" style="margin-top:6px;">
            <span aria-hidden="true" class="stars">
              <span v-for="n in 5" :key="n" :class="{ active: n <= Number(o.rating) }" class="star">★</span>
            </span>
            <span class="rating-text">{{ Number(o.rating).toFixed(1) }}/5</span>
            <button class="btn btn-sm" style="margin-left:8px;" type="button" @click="openReview(o)">Посмотреть отзыв
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Общая модалка чата -->
  <ChatModal v-if="messageModal.visible" :order="messageModal.order" role="client" @close="closeMessage"/>
  <ReviewModal v-if="reviewModal.visible" :order="reviewModal.order" @close="closeReview"
               @submitted="onReviewSubmitted"/>
  <ReviewTextModal v-if="reviewTextModal.visible" :order="reviewTextModal.order" @close="closeReviewText"/>
</template>

<script setup>
import {onBeforeUnmount, onMounted, ref} from 'vue';
import {formatLocalDateTime} from '@/utils/datetime';
import {useOrderStore} from '@/store/order';
import {getNotificationsClient} from '@/services/notifications';
import ChatModal from '@/components/ChatModal.vue';
import {useNotificationsStore} from '@/store/notifications';
import ReviewModal from '@/components/ReviewModal.vue';
import ReviewTextModal from '@/components/ReviewTextModal.vue';

const loading = ref(false);
const orders = ref([]);
const orderStore = useOrderStore();
const messageModal = ref({visible: false, order: null});
const reviewModal = ref({visible: false, order: null});
const reviewTextModal = ref({visible: false, order: null});
const nStore = useNotificationsStore();

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
    return formatLocalDateTime(iso);
  } catch {
    return String(iso ?? '');
  }
}

function isActive(status) {
  const s = String(status || '').toUpperCase();
  const active = new Set(['QUEUED', 'PREPARING', 'READY_FOR_PICKUP', 'OUT_FOR_DELIVERY', 'DELIVERED']);
  return active.has(s);
}

function isCompleted(status) {
  const s = String(status || '').toUpperCase();
  return s === 'COMPLETED';
}

function openMessage(order) {
  try {
    // Помечаем чат как активный, чтобы не накапливать непрочитанные во время просмотра
    nStore.setActive(order?.id);
    // Сбрасываем счётчик непрочитанных по конкретному заказу
    nStore.clearOrder(order?.id);
    // Клиент увидел сообщения — убираем nav-dot с аватарки только если больше нет непрочитанных
    if (!nStore.hasAnyUnread) {
      nStore.clearClientNavDot();
    }
  } catch (_) {
  }
  messageModal.value = {visible: true, order};
}

function closeMessage() {
  messageModal.value.visible = false;
  try {
    nStore.clearActive();
  } catch (_) {
  }
}

function openReview(order) {
  try {
    console.log('[MyOrders] openReview orderId=', order?.id);
  } catch (_) {
  }
  reviewModal.value = {visible: true, order};
}

function closeReview() {
  reviewModal.value.visible = false;
}

function reviewed(orderId) {
  try {
    return localStorage.getItem('reviewed_' + orderId) === '1';
  } catch (_) {
    return false;
  }
}

function onReviewSubmitted(orderId) {
  try {
    localStorage.setItem('reviewed_' + orderId, '1');
  } catch (_) {
  }
  // Обновим список заказов, чтобы подтянулись rating/reviewComment
  try {
    orderStore.fetchOrders().then(() => {
      orders.value = orderStore.orders;
    });
  } catch (_) {
  }
  closeReview();
}

function hasReview(order) {
  if (!order) return false;
  const rated = Number.isFinite(Number(order.rating)) && Number(order.rating) > 0;
  return rated || reviewed(order.id);
}

function openReviewText(order) {
  reviewTextModal.value = {visible: true, order};
}

function closeReviewText() {
  reviewTextModal.value.visible = false;
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
      const t = String(evt?.type || '').toUpperCase();
      if (!evt?.orderId) return;
      if (t.includes('MESSAGE')) {
        // Входящее сообщение от персонала — отмечаем непрочитанным для клиента
        if (!nStore.isActive(evt.orderId)) nStore.markUnread(evt.orderId, 1);
      } else if (t === 'ORDER_STATUS_CHANGED') {
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
