<template>
  <Modal :is-modal-visible="true" :width="560" :z-index="20000" @close="close">
    <template #header>
      <h3>Чат по заказу #{{ order?.id }}</h3>
    </template>
    <template #content>
      <div class="chat-modal">
        <div class="chat">
          <div v-if="loading" class="muted">Загрузка...</div>
          <div v-else ref="listEl" class="chat__list">
            <div
                v-for="m in messages"
                :key="m.id || m._tmpId"
                :class="['msg', m.fromClient ? 'from-client' : 'from-admin', isOwn(m) ? 'is-own' : '']"
            >
              <div class="meta">
                <span class="who">{{ displayWho(m) }}</span>
                <span class="at">{{ formatDate(m.createdAt) }}</span>
              </div>
              <div class="bubble">{{ m.text }}</div>
            </div>
          </div>
        </div>

        <div v-if="isActive(props.order?.status)" class="composer">
          <textarea
              ref="inputEl"
              v-model="text"
              class="input"
              placeholder="Введите сообщение"
              rows="3"
              @keydown="onKeyDown"
          ></textarea>
          <div class="actions">
            <div class="send-hint">Ctrl+Enter — отправить</div>
            <button :disabled="sending || !canSend" class="btn" @click="send">Отправить</button>
          </div>
        </div>
        <div v-else class="muted" style="padding: 12px 0;">Заказ завершён или отменён. Сообщения недоступны.</div>
      </div>
    </template>
  </Modal>
</template>

<script setup>
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import orderAdminService from '@/services/orderAdminService';
import orderClientService from '@/services/orderClientService';
import {getNotificationsClient} from '@/services/notifications';
import {useNotificationsStore} from '@/store/notifications';
import {useAuthStore} from '@/store/auth';
import Modal from '@/components/Modal.vue';
import {formatLocalDateTime} from '@/utils/datetime';

const props = defineProps({
  order: {type: Object, required: true},
  role: {type: String, required: true} // 'admin' | 'client'
});
const emit = defineEmits(['close']);

const loading = ref(false);
const messages = ref([]);
const text = ref('');
const sending = ref(false);
const listEl = ref(null);
const inputEl = ref(null);
let unsubscribe = null;
const nStore = useNotificationsStore();
let io;
const seenIds = new Set(); // messageId для дедупликации
const authStore = useAuthStore();

const canSend = computed(() => !!text.value.trim() && isActive(props.order?.status));

function isActive(status) {
  return status !== 'COMPLETED' && status !== 'CANCELED';
}

function onKeyDown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault();
    if (!sending.value && canSend.value) send();
  }
}

function scrollToBottom() {
  nextTick(() => {
    try {
      const el = listEl.value;
      if (el) el.scrollTop = el.scrollHeight;
    } catch {
    }
  });
}

function formatDate(iso) {
  try {
    return formatLocalDateTime(iso);
  } catch {
    return '';
  }
}

function initials(firstName, patronymic) {
  const f = String(firstName || '').trim();
  const p = String(patronymic || '').trim();
  const fi = f ? f[0].toUpperCase() + '.' : '';
  const pi = p ? p[0].toUpperCase() + '.' : '';
  return (fi + pi).trim();
}

function isOwn(m) {
  // Свои сообщения: для админа — те, что не fromClient; для клиента — те, что fromClient
  return props.role === 'admin' ? !m.fromClient : !!m.fromClient;
}

function displayWho(m) {
  try {
    if (m.fromClient) {
      // Полное ФИО клиента, если доступно в заказе
      const name = props.order?.clientName || [
        props.order?.lastName,
        props.order?.firstName,
        props.order?.patronymic,
      ].filter(Boolean).join(' ');
      return name || 'Клиент';
    }
    // Сообщение от администратора/оператора
    if (props.role === 'admin') {
      const u = authStore?.user || {};
      const ln = u.lastName || '';
      const ini = initials(u.firstName, u.patronymic);
      return (ln ? ln : 'Админ') + (ini ? ' ' + ini : '');
    }
    // Для клиента пробуем вытащить имя оператора из заказа (если бэкенд отдаёт)
    const staffName = props.order?.adminName
        || props.order?.operatorName
        || props.order?.managerName
        || props.order?.handlerName
        || '';
    if (staffName) {
      // Преобразуем в формат «Фамилия И.О.» при наличии
      const parts = String(staffName).trim().split(/\s+/);
      if (parts.length >= 2) {
        const last = parts[0];
        const fi = parts[1]?.[0] ? parts[1][0].toUpperCase() + '.' : '';
        const pi = parts[2]?.[0] ? parts[2][0].toUpperCase() + '.' : '';
        return `${last} ${fi}${pi}`.trim();
      }
      return staffName;
    }
    return 'Админ';
  } catch (_) {
    return m.fromClient ? 'Клиент' : 'Админ';
  }
}

async function loadHistory() {
  loading.value = true;
  try {
    const {data} = await orderAdminService.getMessages(props.order.id);
    messages.value = Array.isArray(data) ? data : [];
    try {
      // Заполним seenIds уже загруженными id, чтобы не дублировать при приходе события
      seenIds.clear();
      for (const m of messages.value) {
        if (m && m.id != null) seenIds.add(Number(m.id));
      }
    } catch (_) {
    }
    scrollToBottom();
    // После первичной загрузки — очищаем индикатор по этому заказу
    try {
      nStore.clearOrder(props.order?.id);
    } catch (_) {
    }
  } catch (e) {
    messages.value = [];
  } finally {
    loading.value = false;
  }
}

async function send() {
  const txt = text.value.trim();
  if (!txt) return;
  sending.value = true;
  try {
    if (props.role === 'admin') {
      await orderAdminService.sendMessage(props.order.id, txt);
      // Мгновенно добавим в локальный список (как от админа)
      messages.value.push({
        _tmpId: 'tmp-' + Date.now(),
        fromClient: false,
        text: txt,
        createdAt: new Date().toISOString()
      });
    } else {
      await orderClientService.sendMessage(props.order.id, txt);
      // Мгновенно добавим в локальный список (как от клиента)
      messages.value.push({
        _tmpId: 'tmp-' + Date.now(),
        fromClient: true,
        text: txt,
        createdAt: new Date().toISOString()
      });
    }
    text.value = '';
    scrollToBottom();
  } catch (e) {
    // можно отобразить toast/ошибку
  } finally {
    sending.value = false;
  }
}

function close() {
  emit('close');
}

onMounted(() => {
  loadHistory();
  const client = getNotificationsClient();
  client.start();
  unsubscribe = client.subscribe((evt) => {
    try {
      if (!evt || evt.orderId !== props.order.id) return;
      if (evt.type === 'COURIER_MESSAGE' && props.role === 'client') {
        if (evt.messageId != null && seenIds.has(Number(evt.messageId))) return;
        messages.value.push({
          _tmpId: 'evt-' + Date.now(),
          fromClient: false,
          text: evt.text,
          createdAt: new Date().toISOString()
        });
        if (evt.messageId != null) seenIds.add(Number(evt.messageId));
        scrollToBottom();
      } else if (evt.type === 'CLIENT_MESSAGE' && props.role === 'admin') {
        if (evt.messageId != null && seenIds.has(Number(evt.messageId))) return;
        messages.value.push({
          _tmpId: 'evt-' + Date.now(),
          fromClient: true,
          text: evt.text,
          createdAt: new Date().toISOString()
        });
        if (evt.messageId != null) seenIds.add(Number(evt.messageId));
        scrollToBottom();
      }
    } catch {
    }
  });
  // Помечаем активный заказ, чтобы входящие не увеличивали непрочитанные
  try {
    nStore.setActive(props.order?.id);
  } catch (_) {
  }
  // И сразу очистим индикатор (на случай, если кнопка открыта вручную без IO)
  try {
    nStore.clearOrder(props.order?.id);
  } catch (_) {
  }
  // Авто-сброс, если контент чата виден хотя бы на 10%
  try {
    io = new IntersectionObserver((entries) => {
      const e = entries && entries[0];
      if (e && e.isIntersecting && e.intersectionRatio >= 0.1) {
        try {
          nStore.clearOrder(props.order?.id);
        } catch (_) {
        }
      }
    }, {threshold: [0, 0.1, 0.25, 0.5, 1]});
    if (listEl.value) io.observe(listEl.value);
  } catch (_) {
  }
});

onBeforeUnmount(() => {
  if (typeof unsubscribe === 'function') unsubscribe();
  try {
    nStore.clearActive();
  } catch (_) {
  }
  try {
    if (io) io.disconnect();
  } catch (_) {
  }
});

watch(() => props.order?.id, () => {
  if (props.order?.id) loadHistory();
});
</script>

<!-- Styles migrated to global theme.css (.chat-modal namespace) -->
