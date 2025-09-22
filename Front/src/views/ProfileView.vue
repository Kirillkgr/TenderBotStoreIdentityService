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
        <OrderListItem
            v-for="order in orderStore.orders"
            :key="order.id"
            :order="order"
        />
      </div>
    </div>

    <!-- Локальная модалка редактирования профиля -->
    <ProfileEditModal v-if="showEdit" @close="showEdit = false" @success="showEdit = false"/>
  </div>

</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useAuthStore} from '../store/auth';
import {useOrderStore} from '../store/order';
import OrderListItem from '../components/OrderListItem.vue';
import ProfileEditModal from '../components/modals/ProfileEditModal.vue';

const authStore = useAuthStore();
const orderStore = useOrderStore();
const showEdit = ref(false);

const formattedDateOfBirth = computed(() => {
  if (!authStore.user || !authStore.user.dateOfBirth) return '';
  const [year, month, day] = authStore.user.dateOfBirth.split('-');
  return `${day}.${month}`;
});

function formatDate(val) {
  try {
    return val ? new Date(val).toLocaleString() : '—';
  } catch {
    return '—';
  }
}

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
