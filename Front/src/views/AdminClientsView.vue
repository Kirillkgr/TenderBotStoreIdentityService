<template>
  <div class="admin-page">
    <h2>Клиенты</h2>
    <p class="text-muted">Страница-заготовка. Бэкенд-API для клиентов будет подключен позже.</p>

    <div class="toolbar">
      <input v-model="search" class="input" placeholder="Поиск (имя, email, телефон)"/>
      <input v-model.number="masterId" class="input" min="0" placeholder="Master ID" type="number"/>
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
        <th>Имя</th>
        <th>Email</th>
        <th>Телефон</th>
        <th>Мастер</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="c in paged" :key="c.id">
        <td>{{ c.id }}</td>
        <td>{{ c.fullName || [c.lastName, c.firstName, c.patronymic].filter(Boolean).join(' ') }}</td>
        <td>{{ c.email || '—' }}</td>
        <td>{{ c.phone || '—' }}</td>
        <td>{{ c.masterName || c.masterId || '—' }}</td>
      </tr>
      <tr v-if="filtered.length === 0">
        <td class="text-muted" colspan="5">Нет данных</td>
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
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import * as clientService from '@/services/clientService';
// TODO: replace with backend admin endpoint when ready
const clients = ref([]);
const loading = ref(false);
const error = ref('');
const search = ref('');
const masterId = ref();
const page = ref(1); // UI starts at 1
const pageSize = ref(10);
const totalPages = ref(1);

async function reload() {
  loading.value = true;
  error.value = '';
  try {
    const params = {
      page: Math.max(0, (page.value || 1) - 1),
      size: pageSize.value || 10,
      search: (search.value || '').trim() || undefined,
      masterId: masterId.value || undefined,
    };
    const res = await clientService.getAdminClients(params);
    const body = res?.data ?? {};
    if (Array.isArray(body)) {
      clients.value = body;
      totalPages.value = 1;
    } else if (Array.isArray(body.content)) {
      clients.value = body.content || [];
      totalPages.value = Math.max(1, Number(body.totalPages) || 1);
      if (typeof body.number === 'number') page.value = Number(body.number) + 1;
      if (typeof body.size === 'number') pageSize.value = Number(body.size) || pageSize.value;
    } else if (Array.isArray(body.data)) {
      clients.value = body.data || [];
      totalPages.value = 1;
    } else {
      clients.value = [];
      totalPages.value = 1;
    }
  } catch (e) {
    error.value = 'API для клиентов будет подключено позже';
    clients.value = [];
  } finally {
    loading.value = false;
  }
}

onMounted(reload);

const filtered = computed(() => {
  let arr = clients.value || [];
  if (search.value) {
    const s = search.value.toLowerCase();
    arr = arr.filter(c => String(c.fullName || [c.lastName, c.firstName, c.patronymic].filter(Boolean).join(' ')).toLowerCase().includes(s)
        || String(c.email || '').toLowerCase().includes(s)
        || String(c.phone || '').toLowerCase().includes(s));
  }
  if (masterId.value != null && masterId.value !== '') {
    arr = arr.filter(c => Number(c.masterId) === Number(masterId.value));
  }
  return arr;
});

const paged = computed(() => {
  // если сервер вернул уже страницу — показываем её
  if (totalPages.value >= 1) {
    return filtered.value.length === clients.value.length ? clients.value : filtered.value;
  }
  // fallback: клиентский срез
  const start = (page.value - 1) * pageSize.value;
  return filtered.value.slice(start, start + pageSize.value);
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
