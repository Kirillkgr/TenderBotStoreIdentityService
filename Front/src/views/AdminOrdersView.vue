<template>
  <div class="admin-page">
    <h2>Заказы</h2>
    <p class="text-muted">Страница-заготовка. Бэкенд-API для заказов будет подключен позже.</p>

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
        <th>Клиент</th>
        <th>Бренд</th>
        <th>Сумма</th>
        <th>Создан</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="o in paged" :key="o.id">
        <td>{{ o.id }}</td>
        <td>{{ o.clientName || '—' }}</td>
        <td>{{ o.brandName || o.brandId || '—' }}</td>
        <td>{{ formatPrice(o.total) }} ₽</td>
        <td>{{ formatLocalDateTime(o.createdAt) }}</td>
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
import {formatLocalDateTime} from '@/utils/datetime';
import * as orderService from '@/services/orderService';

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

async function reload() {
  loading.value = true;
  error.value = '';
  try {
    // Серверная пагинация (fallback на массив при заглушке)
    const params = {
      page: Math.max(0, (page.value || 1) - 1),
      size: pageSize.value || 10,
      search: (search.value || '').trim() || undefined,
      brandId: brandId.value || undefined,
      dateFrom: dateFrom.value || undefined,
      dateTo: dateTo.value || undefined,
    };
    const res = await orderService.getAdminOrders(params);
    const body = res?.data ?? {};
    if (Array.isArray(body)) {
      orders.value = body;
      totalPages.value = 1;
    } else if (Array.isArray(body.content)) {
      orders.value = body.content || [];
      totalPages.value = Math.max(1, Number(body.totalPages) || 1);
      // синхронизация страницы и размера, если сервер вернул другие значения
      if (typeof body.number === 'number') page.value = Number(body.number) + 1;
      if (typeof body.size === 'number') pageSize.value = Number(body.size) || pageSize.value;
    } else if (Array.isArray(body.data)) {
      orders.value = body.data || [];
      totalPages.value = 1;
    } else {
      orders.value = [];
      totalPages.value = 1;
    }
  } catch (e) {
    error.value = 'API для заказов будет подключено позже';
    orders.value = [];
  } finally {
    loading.value = false;
  }
}

onMounted(reload);

const filtered = computed(() => {
  let arr = orders.value || [];
  if (search.value) {
    const s = search.value.toLowerCase();
    arr = arr.filter(o => String(o.id).includes(s) || String(o.clientName || '').toLowerCase().includes(s) || String(o.brandName || o.brandId || '').toLowerCase().includes(s));
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
      if (to && d > to) return false;
      return true;
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
