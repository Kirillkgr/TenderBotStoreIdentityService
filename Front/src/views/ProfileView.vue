<template>
  <div class="profile-container">
    <div v-if="authStore.user" class="profile-card">
      <div class="profile-card__header">
        <div>
          <h1 class="profile-title">Профиль</h1>
          <div class="profile-sub">ID: {{ authStore.user.id || '—' }}</div>
        </div>
        <button class="btn btn-primary" @click="showEdit = true">
          <i class="bi bi-pencil me-1"></i>
          Изменить
        </button>
      </div>

      <div class="profile-grid">
        <div class="col">
          <div class="field"><span class="label">Фамилия</span><span class="val">{{
              authStore.user.lastName || '—'
            }}</span></div>
          <div class="field"><span class="label">Имя</span><span class="val">{{
              authStore.user.firstName || '—'
            }}</span></div>
          <div class="field"><span class="label">Отчество</span><span class="val">{{
              authStore.user.patronymic || '—'
            }}</span></div>
          <div class="field"><span class="label">Дата рождения</span><span class="val">{{
              formattedDateOfBirth || '—'
            }}</span></div>
          <div class="field"><span class="label">Создан</span><span class="val">{{
              formatDate(authStore.user.createdAt)
            }}</span></div>
        </div>
        <div class="col">
          <div class="field"><span class="label">Email</span><span class="val">{{ authStore.user.email || '—' }}</span>
          </div>
          <div class="field"><span class="label">Логин</span><span class="val">{{
              authStore.user.username || '—'
            }}</span></div>
          <div class="field"><span class="label">Телефон</span><span class="val">{{
              authStore.user.phone || '—'
            }}</span></div>
          <div class="field"><span class="label">Роли</span><span
              class="val">{{ (authStore.user.roles || []).join(', ') || '—' }}</span></div>
          <div class="field"><span class="label">Обновлён</span><span
              class="val">{{ formatDate(authStore.user.updatedAt) }}</span></div>
        </div>
      </div>
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
        <div v-for="order in orderStore.orders" :key="order.id" class="order-row">
          <OrderListItem :order="order"
                         @open-message="openMessage"
                         @open-review="openReview"
                         @open-review-text="openReview"/>
        </div>
      </div>
    </div>

    <!-- Локальная модалка редактирования профиля -->
    <ProfileEditModal v-if="showEdit" @close="showEdit = false" @success="showEdit = false"/>

    <!-- Общая модалка чата -->
    <ChatModal v-if="messageModal.visible" :order="messageModal.order" role="client" @close="closeMessage"/>

    <!-- Модалки отзывов -->
    <ReviewModal v-if="reviewModal.visible" :order="reviewModal.order" @close="closeReview"
                 @submitted="onReviewSubmitted"/>
    <ReviewTextModal v-if="reviewTextModal.visible" :order="reviewTextModal.order" @close="closeReviewText"/>
  </div>

</template>

<script setup>
import {computed, onBeforeUnmount, onMounted, ref} from 'vue';
import {useAuthStore} from '../store/auth';
import {useOrderStore} from '../store/order';
import OrderListItem from '../components/OrderListItem.vue';
import ProfileEditModal from '../components/modals/ProfileEditModal.vue';
import orderClientService from '@/services/orderClientService';
import {getNotificationsClient} from '@/services/notifications';
import ChatModal from '@/components/ChatModal.vue';
import ReviewModal from '@/components/ReviewModal.vue';
import ReviewTextModal from '@/components/ReviewTextModal.vue';
import {formatLocalDateTime} from '@/utils/datetime';

const authStore = useAuthStore();
const orderStore = useOrderStore();
const showEdit = ref(false);

// Модалка чата по выбранному заказу (общий ChatModal)
const messageModal = ref({visible: false, order: null});
// Модалки отзывов
const reviewModal = ref({visible: false, order: null});
const reviewTextModal = ref({visible: false, order: null});

const formattedDateOfBirth = computed(() => {
  if (!authStore.user || !authStore.user.dateOfBirth) return '';
  const [year, month, day] = authStore.user.dateOfBirth.split('-');
  return `${day}.${month}`;
});

function formatDate(val) {
  try {
    return val ? formatLocalDateTime(val) : '—';
  } catch {
    return '—';
  }
}

onMounted(() => {
  orderStore.fetchOrders();
});

// Подписка на события long-poll: сообщения курьера и смена статуса заказа
let unsubscribe = null;
onMounted(() => {
  const client = getNotificationsClient();
  client.start();
  unsubscribe = client.subscribe((evt) => {
    try {
      if (evt?.type === 'COURIER_MESSAGE' && evt.orderId) {
        // Откроем модалку для соответствующего заказа, если он активен
        const order = (orderStore.orders || []).find(o => o.id === evt.orderId);
        if (order && isActive(order.status)) {
          openMessage(order);
        }
      } else if (evt?.type === 'ORDER_STATUS_CHANGED' && evt.orderId) {
        // Обновим список заказов, чтобы отразить новый статус
        orderStore.fetchOrders();
      }
    } catch (_) {
    }
  });
});

onBeforeUnmount(() => {
  if (typeof unsubscribe === 'function') unsubscribe();
});

function isActive(status) {
  return status !== 'COMPLETED' && status !== 'CANCELED';
}

function openMessage(order) {
  messageModal.value = {visible: true, order, text: '', error: ''};
}

function closeMessage() {
  messageModal.value.visible = false;
}

function openReview(order) {
  reviewModal.value = {visible: true, order};
}

function closeReview() {
  reviewModal.value.visible = false;
}

function onReviewSubmitted() {
  // После успешной отправки подтянуть обновлённые данные заказов
  try {
    orderStore.fetchOrders();
  } catch (_) {
  }
  closeReview();
}

function closeReviewText() {
  reviewTextModal.value.visible = false;
}

async function sendMessage() {
  const {order, text} = messageModal.value;
  if (!order || !text || !text.trim()) {
    messageModal.value.error = 'Введите сообщение';
    return;
  }
  sending.value = true;
  try {
    await orderClientService.sendMessage(order.id, text.trim());
    closeMessage();
  } catch (e) {
    messageModal.value.error = 'Не удалось отправить сообщение';
  } finally {
    sending.value = false;
  }
}
</script>

<style scoped>
.profile-container {
  max-width: 800px;
  margin: auto;
  padding: 2rem;
  text-align: left; /* Улучшение читаемости */
}

.profile-card {
  background: var(--card);
  border: 1px solid var(--card-border);
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 24px;
}

.profile-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-title {
  margin: 0 0 6px;
}

.profile-sub {
  color: var(--muted);
  font-size: 12px;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 24px;
  margin-top: 10px;
}

.profile-grid .col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field {
  display: flex;
  gap: 10px;
}

.label {
  color: var(--muted);
  min-width: 120px;
}

.val {
  color: var(--text);
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
