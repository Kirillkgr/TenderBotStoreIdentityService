<template>
  <div class="admin-page">
    <h2>Заказы</h2>

    <div class="toolbar">
      <input v-model="search" class="input" placeholder="Поиск (id, клиент, бренд)"/>
      <input v-model.number="brandId" class="input" min="0" placeholder="Brand ID" type="number"/>
      <input v-model="dateFrom" class="input" type="date"/>
      <input v-model="dateTo" class="input" type="date"/>
      <button :disabled="loading" class="btn btn-sm" @click="reload">
        <i class="bi bi-arrow-repeat"></i> Обновить
      </button>
    </div>

    <div v-if="loading" class="info">Загрузка...</div>
    <div v-else-if="error" class="error">{{ error }}</div>

    <table v-else class="table">
      <thead>
      <tr>
        <th>ID</th>
        <th>Бренд</th>
        <th>Клиент (ФИО)</th>
        <th>Контакты</th>
        <th>Товары</th>
        <th>Новые</th>
        <th>Комментарий</th>
        <th>Сумма</th>
        <th>Статус</th>
        <th>Создан</th>
        <th>Действия</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="o in paged" :key="o.id"
          :class="{ 'row-unread': nStore.hasUnreadByOrder(o.id), 'row-queued': nStore.isQueued(o.id) || String(o.status).toUpperCase()==='QUEUED' }">
        <td class="id-cell">
          <span class="id-text">{{ o.id }}</span>
          <span v-if="nStore.isQueued(o.id) || String(o.status).toUpperCase()==='QUEUED'" class="queued-dot"
                title="Новый заказ"></span>
        </td>
        <td>{{ o.brandName || o.brandId || '—' }}</td>
        <td>{{ o.clientName || '—' }}</td>
        <td>
          <div>{{ o.clientPhone || '—' }}</div>
          <div v-if="o.clientEmail && !o.clientPhone" class="text-muted">{{ o.clientEmail }}</div>
        </td>
        <td style="max-width: 320px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
          {{ formatItems(o.items) }}
        </td>
        <td class="text-center" style="min-width:70px;">{{ nStore.countByOrder(o.id) }}</td>
        <td style="max-width: 240px; word-break: break-word;">{{ o.comment || '—' }}</td>
        <td>{{ formatPrice(o.total) }} ₽</td>
        <td>
          <select v-model="statusDraft[o.id]" class="input" style="min-width:160px;">
            <option v-for="st in statusesFor(o.status)" :key="st" :value="st">{{ st }}</option>
          </select>
          <button
              v-can="{ any: ['ADMIN','OWNER','CASHIER'], mode: 'disable', tooltip: 'Нет прав изменять статус' }"
              :disabled="statusDraft[o.id]===o.status || savingStatusId===o.id"
              class="btn btn-sm"
                  @click="applyStatus(o)">
            Сохранить
          </button>
        </td>
        <td>{{ formatLocalDateTime(o.createdAt) }}</td>
        <td>
          <button v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Нет прав отправлять сообщения' }"
                  class="btn btn-sm btn-chat" type="button" @click.stop="openMessage(o)">
            Сообщение
            <span v-if="nStore.hasUnreadByOrder(o.id)" class="btn-unread-dot" title="Есть новые сообщения"></span>
          </button>
          <div v-if="o.rating" class="rating-wrap">
            <span aria-hidden="true" class="stars">
              <span v-for="n in 5" :key="n" :class="{ active: n <= Number(o.rating) }" class="star">★</span>
            </span>
            <span class="rating-text">{{ Number(o.rating).toFixed(1) }}/5</span>
          </div>
          <div v-else class="rating-wrap muted">Нет оценки</div>
          <button v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Нет прав просматривать отзывы' }"
                  class="btn btn-sm" style="margin-top:6px;" type="button"
                  @click="openReviewText(o)">Отзыв
          </button>
        </td>
      </tr>
      <tr v-if="(orders||[]).length === 0">
        <td class="text-muted" colspan="9">Нет данных</td>
      </tr>
      </tbody>
    </table>

    <div v-if="filtered.length > 0" class="pager">
      <button :disabled="page===1" class="btn btn-sm" @click="prevPage">Назад</button>
      <span>Стр. {{ page }} из {{ totalPages }}</span>
      <button :disabled="page===totalPages" class="btn btn-sm" @click="nextPage">Вперёд</button>
      <select v-model.number="pageSize" class="input">
        <option :value="10">10</option>
        <option :value="20">20</option>
        <option :value="50">50</option>
      </select>
    </div>

    <!-- Модалка отправки сообщения клиенту -->
    <ChatModal v-if="messageModal.visible" :order="messageModal.order" role="admin" @close="closeMessage"/>
    <ReviewTextModal v-if="reviewTextModal.visible" :order="reviewTextModal.order" @close="closeReviewText"/>
  </div>
</template>

<script setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import {formatLocalDateTime} from '@/utils/datetime';
import orderAdminService, {ORDER_STATUSES} from '@/services/orderAdminService';
import {getNotificationsClient} from '@/services/notifications';
import ChatModal from '@/components/ChatModal.vue';
import ReviewTextModal from '@/components/ReviewTextModal.vue';
import {useNotificationsStore} from '@/store/notifications';

const orders = ref([]);
const loading = ref(false);
const error = ref('');
const search = ref('');
const brandId = ref();
const dateFrom = ref('');
const dateTo = ref('');
const page = ref(1); // UI starts at 1
const pageSize = ref(10);
const totalPages = ref(1);

function formatPrice(val) {
  const n = Number(val);
  if (!Number.isFinite(n)) return '—';
  return new Intl.NumberFormat('ru-RU', {minimumFractionDigits: 2, maximumFractionDigits: 2}).format(n);
}


function formatItems(items) {
  try {
    const arr = Array.isArray(items) ? items : [];
    if (arr.length === 0) return '—';
    const head = arr.slice(0, 3).map(it => {
      const name = it?.productName || ('Товар #' + (it?.productId ?? ''));
      const qty = Number(it?.quantity || 0);
      return `${name} × ${qty}`;
    }).join(', ');
    const rest = arr.length - 3;
    return rest > 0 ? `${head} +${rest} ещё` : head;
  } catch {
    return '—';
  }
}

const savingStatusId = ref(null);
const statusDraft = ref({}); // orderId -> status

function statusesFor(current) {
  const map = {
    QUEUED: ['QUEUED', 'PREPARING', 'CANCELED'],
    PREPARING: ['PREPARING', 'READY_FOR_PICKUP', 'OUT_FOR_DELIVERY', 'CANCELED'],
    READY_FOR_PICKUP: ['READY_FOR_PICKUP', 'DELIVERED', 'COMPLETED', 'CANCELED'],
    OUT_FOR_DELIVERY: ['OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELED'],
    DELIVERED: ['DELIVERED', 'COMPLETED'],
    COMPLETED: ['COMPLETED'],
    CANCELED: ['CANCELED'],
  };
  return map[current] || ORDER_STATUSES;
}

async function reload() {
  loading.value = true;
  error.value = '';
  try {
    const params = {
      page: Math.max(0, (page.value || 1) - 1),
      size: pageSize.value || 10,
      search: (search.value || '').trim() || undefined,
      brandId: brandId.value || undefined,
      dateFrom: dateFrom.value || undefined,
      dateTo: dateTo.value || undefined,
    };
    const res = params.brandId == null
        ? await orderAdminService.listAccessibleOrders(params.page, params.size, undefined, params.search, params.dateFrom, params.dateTo)
        : await orderAdminService.listBrandOrders(params.brandId, params.page, params.size);
    const body = res?.data ?? {};
    if (Array.isArray(body.content)) {
      orders.value = body.content || [];
      try {
        nStore.resetQueuedFromList(orders.value);
      } catch (_) {
      }
      totalPages.value = Math.max(1, Number(body.totalPages) || 1);
      // синхронизация страницы и размера, если сервер вернул другие значения
      if (typeof body.number === 'number') page.value = Number(body.number) + 1;
      if (typeof body.size === 'number') pageSize.value = Number(body.size) || pageSize.value;
    } else {
      orders.value = [];
      totalPages.value = 1;
    }
    // Инициализируем драфты статусов
    const map = {};
    for (const o of orders.value) map[o.id] = o.status;
    statusDraft.value = map;
  } catch (e) {
    error.value = e?.response?.data?.message || 'Не удалось загрузить заказы';
    orders.value = [];
  } finally {
    loading.value = false;
  }
}

onMounted(reload);

let unsubscribe = null;
const nStore = useNotificationsStore();
onMounted(() => {
  const client = getNotificationsClient();
  client.start();
  unsubscribe = client.subscribe((evt) => {
    try {
      if (evt?.type === 'CLIENT_MESSAGE' && evt.orderId) {
        const order = (orders.value || []).find(o => o.id === evt.orderId);
        if (order) {
          // Простая реакция: автооткрываем модалку отправки сообщения
          openMessage(order);
        }
      } else if (evt?.type === 'ORDER_STATUS_CHANGED' && evt.orderId) {
        // Обновим строку заказа или перезагрузим страницу
        const idx = (orders.value || []).findIndex(o => o.id === evt.orderId);
        if (idx >= 0) {
          // Патчим статус локально для мгновенного UX
          const next = [...orders.value];
          next[idx] = {...next[idx], status: evt.newStatus};
          orders.value = next;
        } else {
          // Если заказ не найден на текущей странице — можно перезагрузить
          reload();
        }
      } else if (evt?.type === 'NEW_ORDER' && evt.orderId) {
        // Новый заказ: перезагружаем список и проигрываем короткий звук
        reload();
        try {
          playBeep();
        } catch (_) {
        }
      }
    } catch (_) {
    }
  });
});

onBeforeUnmount(() => {
  if (typeof unsubscribe === 'function') unsubscribe();
});

const filtered = computed(() => {
  let arr = orders.value || [];
  if (search.value) {
    const s = search.value.toLowerCase();
    arr = arr.filter(o => String(o.id).includes(s)
        || String(o.clientName || '').toLowerCase().includes(s)
        || String(o.clientPhone || '').toLowerCase().includes(s)
        || String(o.clientEmail || '').toLowerCase().includes(s)
        || String(o.brandName || o.brandId || '').toLowerCase().includes(s));
  }
  if (brandId.value != null && brandId.value !== '') {
    arr = arr.filter(o => Number(o.brandId) === Number(brandId.value));
  }
  // клиентская фильтрация по дате (если есть поля createdAt ISO)
  const from = dateFrom.value ? new Date(dateFrom.value) : null;
  const to = dateTo.value ? new Date(dateTo.value + 'T23:59:59') : null;
  if (from || to) {
    arr = arr.filter(o => {
      const d = o.createdAt ? new Date(o.createdAt) : null;
      if (!d || isNaN(d)) return false;
      if (from && d < from) return false;
      return !(to && d > to);

    });
  }
  return arr;
});

// Пока серверная пагинация: отображаем как есть (orders уже страница). Для заглушек оставляем клиентский срез.
const paged = computed(() => {
  // если сервер вернул уже страницу (обычный случай) — показываем orders
  if (totalPages.value >= 1 && (search.value || brandId.value || dateFrom.value || dateTo.value || true)) {
    return orders.value || [];
  }
  // fallback: клиентский срез
  const start = (page.value - 1) * pageSize.value;
  return (orders.value || []).slice(start, start + pageSize.value);
});

function prevPage() {
  if (page.value > 1) {
    page.value -= 1;
    reload();
  }
}

function nextPage() {
  if (page.value < totalPages.value) {
    page.value += 1;
    reload();
  }
}

// Когда меняется brandId, сбрасываем на первую страницу
watch(brandId, () => {
  page.value = 1;
});

async function applyStatus(order) {
  const draft = statusDraft.value[order.id];
  if (!draft || draft === order.status) return;
  savingStatusId.value = order.id;
  try {
    await orderAdminService.updateStatus(order.id, draft);
    // Обновим локально
    order.status = draft;
    if (String(draft).toUpperCase() !== 'QUEUED') {
      try {
        nStore.clearQueued(order.id);
      } catch (_) {
      }
    } else {
      try {
        nStore.markQueued(order.id);
      } catch (_) {
      }
    }
  } catch (e) {
    alert(e?.response?.status === 409 ? 'Недопустимый переход статуса' : 'Не удалось сохранить статус');
    statusDraft.value[order.id] = order.status; // откат
  } finally {
    savingStatusId.value = null;
  }
}

const messageModal = ref({visible: false, order: null});
const reviewTextModal = ref({visible: false, order: null});

function openMessage(order) {
  try {
    console.log('[AdminOrders] openMessage orderId=', order?.id);
  } catch (e) {
  }
  try {
    nStore.clearOrder(order?.id);
  } catch (_) {
  }
  messageModal.value = {visible: true, order};
}

function closeMessage() {
  messageModal.value.visible = false;
}

function openReviewText(order) {
  reviewTextModal.value = {visible: true, order};
}

function closeReviewText() {
  reviewTextModal.value.visible = false;
}

// Короткий звуковой сигнал при новом заказе
let audioCtx = null;

function playBeep() {
  try {
    audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)();
    const o = audioCtx.createOscillator();
    const g = audioCtx.createGain();
    o.type = 'sine';
    o.frequency.value = 880; // A5
    g.gain.setValueAtTime(0.0001, audioCtx.currentTime);
    g.gain.exponentialRampToValueAtTime(0.15, audioCtx.currentTime + 0.01);
    g.gain.exponentialRampToValueAtTime(0.0001, audioCtx.currentTime + 0.25);
    o.connect(g).connect(audioCtx.destination);
    o.start();
    o.stop(audioCtx.currentTime + 0.26);
  } catch (e) {
    // ignore if blocked
  }
}
</script>

<style scoped>
.admin-page {
  padding: 16px;
}

.toolbar {
  margin: 8px 0 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table th, .table td {
  border: 1px solid var(--border);
  padding: 8px;
}

.info {
  color: var(--muted);
}

.error {
  color: var(--danger);
}

.text-muted {
  color: var(--muted);
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

.row-unread {
  background: rgba(229, 57, 53, 0.08);
}

.text-center {
  text-align: center;
}

.rating-wrap {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.stars .star {
  color: #888;
}

.stars .star.active {
  color: #ffd54f;
  text-shadow: 0 0 4px rgba(255, 213, 79, .5);
}

.rating-text {
  color: #bbb;
  font-size: .9rem;
}

.input {
  border: 1px solid var(--border);
  background: var(--input-bg);
  color: var(--text);
  border-radius: 8px;
  padding: 6px 10px;
}

.pager {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}
</style>
