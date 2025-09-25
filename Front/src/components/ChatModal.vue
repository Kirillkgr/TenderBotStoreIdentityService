<template>
  <div class="modal" @click.self="close">
    <div class="modal__dialog">
      <div class="modal__header">
        <h3>Чат по заказу #{{ order?.id }}</h3>
        <button class="btn" @click="close">×</button>
      </div>
      <div class="modal__body">
        <div class="chat">
          <div v-if="loading" class="muted">Загрузка...</div>
          <div v-else ref="listEl" class="chat__list">
            <div v-for="m in messages" :key="m.id || m._tmpId"
                 :class="['msg', m.fromClient ? 'from-client':'from-admin']">
              <div class="meta">
                <span class="who">{{ m.fromClient ? 'Клиент' : 'Админ' }}</span>
                <span class="at">{{ formatDate(m.createdAt) }}</span>
              </div>
              <div class="text">{{ m.text }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="modal__footer">
        <textarea v-model="text" class="input" placeholder="Введите сообщение" rows="3"></textarea>
        <div class="actions">
          <button :disabled="sending || !canSend" class="btn" @click="send">Отправить</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {onMounted, onBeforeUnmount, ref, watch, nextTick, computed} from 'vue';
import orderAdminService from '@/services/orderAdminService';
import orderClientService from '@/services/orderClientService';
import {getNotificationsClient} from '@/services/notifications';

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
let unsubscribe = null;

const canSend = computed(() => !!text.value.trim() && isActive(props.order?.status));

function isActive(status) {
  return status !== 'COMPLETED' && status !== 'CANCELED';
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
    const d = new Date(iso);
    if (isNaN(d)) return '';
    return d.toLocaleString('ru-RU');
  } catch {
    return '';
  }
}

async function loadHistory() {
  loading.value = true;
  try {
    const {data} = await orderAdminService.getMessages(props.order.id);
    messages.value = Array.isArray(data) ? data : [];
    scrollToBottom();
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
        messages.value.push({
          _tmpId: 'evt-' + Date.now(),
          fromClient: false,
          text: evt.text,
          createdAt: new Date().toISOString()
        });
        scrollToBottom();
      } else if (evt.type === 'CLIENT_MESSAGE' && props.role === 'admin') {
        messages.value.push({
          _tmpId: 'evt-' + Date.now(),
          fromClient: true,
          text: evt.text,
          createdAt: new Date().toISOString()
        });
        scrollToBottom();
      }
    } catch {
    }
  });
});

onBeforeUnmount(() => {
  if (typeof unsubscribe === 'function') unsubscribe();
});

watch(() => props.order?.id, () => {
  if (props.order?.id) loadHistory();
});
</script>

<style scoped>
.modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal__dialog {
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  width: min(760px, 96vw);
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.modal__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid var(--border);
}

.modal__body {
  padding: 0 12px;
}

.modal__footer {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  align-items: center;
  padding: 12px;
  border-top: 1px solid var(--border);
}

.chat {
  height: 56vh;
  display: flex;
  flex-direction: column;
}

.chat__list {
  flex: 1;
  overflow: auto;
  display: grid;
  gap: 10px;
  padding: 12px 4px;
}

.msg {
  padding: 8px 10px;
  border-radius: 10px;
  background: #2f2f2f;
}

.msg.from-client {
  align-self: flex-start;
  background: #334155;
}

.msg.from-admin {
  align-self: flex-end;
  background: #3f3f3f;
}

.msg .meta {
  font-size: 12px;
  color: #bbb;
  margin-bottom: 4px;
  display: flex;
  gap: 8px;
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

.muted {
  color: #999;
  padding: 12px;
}
</style>
