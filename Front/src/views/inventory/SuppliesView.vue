<template>
  <div class="page supplies">
    <div class="page__head">
      <h1 class="page-title">Поставки</h1>
      <button class="btn btn-primary" type="button" @click="openCreate">Новая поставка</button>
    </div>

    <div class="card">
      <div class="table-wrap desktop-only">
        <table class="tbl">
          <thead>
          <tr>
            <th>ID</th>
            <th>Склад</th>
            <th>Поставщик</th>
            <th>Позиций</th>
            <th>Статус</th>
            <th>Дата</th>
            <th></th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="s in supplies" :key="s.id">
            <td>#{{ s.id }}</td>
            <td>{{ s.warehouseName || s.warehouse?.name || s.warehouseId }}</td>
            <td>{{ supplierName(s.supplierId) }}</td>
            <td>{{ Array.isArray(s.items) ? s.items.length : 0 }}</td>
            <td>
              <span :class="`st-${(s.status||'').toLowerCase()}`" class="badge">{{ s.status || 'DRAFT' }}</span>
            </td>
            <td>{{ fmtDate(s.date) }}</td>
            <td class="actions">
              <button class="btn" @click="openDetails(s)">Подробнее</button>
            </td>
          </tr>
          <tr v-if="!loading && !supplies.length">
            <td class="muted" colspan="6">Нет поставок</td>
          </tr>
          <tr v-if="loading">
            <td class="muted" colspan="6">Загрузка…</td>
          </tr>
          </tbody>
        </table>
      </div>

      <!-- Mobile card list -->
      <div class="mobile-list">
        <div v-for="s in supplies" :key="s.id" class="mcard">
          <div class="mcard__row">
            <div class="mcard__id">#{{ s.id }}</div>
            <div class="mcard__status"><span :class="`st-${(s.status||'').toLowerCase()}`" class="badge">{{
                s.status || 'DRAFT'
              }}</span></div>
          </div>
          <div class="mcard__row">
            <div class="mcard__lbl">Склад</div>
            <div class="mcard__val">{{ s.warehouseName || s.warehouse?.name || s.warehouseId }}</div>
          </div>
          <div class="mcard__row">
            <div class="mcard__lbl">Поставщик</div>
            <div class="mcard__val">{{ supplierName(s.supplierId) }}</div>
          </div>
          <div class="mcard__row">
            <div class="mcard__lbl">Позиций</div>
            <div class="mcard__val">{{ Array.isArray(s.items) ? s.items.length : 0 }}</div>
          </div>
          <div class="mcard__row">
            <div class="mcard__lbl">Дата</div>
            <div class="mcard__val">{{ fmtDate(s.date) }}</div>
          </div>
          <div class="mcard__actions">
            <button class="btn" @click="openDetails(s)">Подробнее</button>
          </div>
        </div>
        <div v-if="!loading && !supplies.length" class="mcard muted">Нет поставок</div>
        <div v-if="loading" class="mcard muted">Загрузка…</div>
      </div>
    </div>

    <div v-if="totalPages > 1" class="pager">
      <button :disabled="page<=1" class="btn" @click="goPrev">Назад</button>
      <span class="muted">Стр. {{ page }} из {{ totalPages }}</span>
      <button :disabled="page>=totalPages" class="btn" @click="goNext">Вперёд</button>
    </div>

    <SuppliesCreateModal :visible="createOpen" @close="createOpen=false" @created="handleCreated"/>
    <SupplyDetailsModal :suppliers="suppliers" :supply="selectedSupply" :visible="detailsOpen"
                        @close="detailsOpen=false"/>
  </div>

</template>

<script setup>
import {onMounted, ref, watch} from 'vue';
import {fetchSuppliesSmart} from '../../services/inventory/suppliesService';
import SuppliesCreateModal from '../../components/inventory/SuppliesCreateModal.vue';
import SupplyDetailsModal from '../../components/inventory/SupplyDetailsModal.vue';
import {useAuthStore} from '../../store/auth';
import {getSuppliers} from '../../services/inventory/supplierService';

const supplies = ref([]);
const loading = ref(false);
const createOpen = ref(false);
const auth = useAuthStore();
// simple pagination state (backend is Page)
const page = ref(1); // 1-based for UI
const pageSize = ref(20);
const totalPages = ref(1);
const detailsOpen = ref(false);
const selectedSupply = ref(null);
const suppliers = ref([]);

function fmtDate(v) {
  if (!v) return '';
  try {
    return new Date(v).toLocaleString();
  } catch {
    return String(v);
  }
}

function supplierName(id) {
  if (!id) return '—';
  const m = suppliers.value.find(x => String(x.id) === String(id));
  return m ? m.name : `#${id}`;
}

async function waitAuthReady(timeoutMs = 1500) {
  const start = Date.now();
  while (auth.isRestoringSession && Date.now() - start < timeoutMs) {
    await new Promise(r => setTimeout(r, 100));
  }
}

async function load() {
  if (!auth.isAuthenticated) {
    // попросим залогиниться и выйдем
    try {
      window.dispatchEvent(new Event('open-login-modal'));
    } catch {
    }
    supplies.value = [];
    return;
  }
  await waitAuthReady();
  loading.value = true;
  try {
    const {data} = await fetchSuppliesSmart({page: Math.max(0, (page.value || 1) - 1), size: pageSize.value});
    const items = Array.isArray(data?.content)
        ? data.content
        : (Array.isArray(data?.items) ? data.items : (Array.isArray(data) ? data : []));
    supplies.value = items;
    if (typeof data?.totalPages === 'number') totalPages.value = Math.max(1, Number(data.totalPages));
    if (typeof data?.number === 'number') page.value = Number(data.number) + 1;
  } catch (e) {
    console.error('load supplies error', e);
    if (e?.response?.status === 401) {
      try {
        window.dispatchEvent(new Event('open-login-modal'));
      } catch {
      }
    }
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  createOpen.value = true;
}

async function handleCreated() {
  createOpen.value = false;
  await load();
}

onMounted(() => {
  if (auth.isAuthenticated) {
    load();
    loadSuppliers();
  }
});
watch(() => auth.isAuthenticated, (v) => {
  if (v) load();
});

function goPrev() {
  if (page.value > 1) {
    page.value--;
    load();
  }
}

function goNext() {
  if (page.value < totalPages.value) {
    page.value++;
    load();
  }
}

async function loadSuppliers() {
  try {
    const {data} = await getSuppliers();
    suppliers.value = Array.isArray(data?.items) ? data.items : (Array.isArray(data) ? data : []);
  } catch (_) {
  }
}

function openDetails(s) {
  selectedSupply.value = s || null;
  detailsOpen.value = !!s;
}
</script>

<style scoped>
.page {
  text-align: left;
}

.page__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.card {
  background: rgba(255, 255, 255, .04);
  border: 1px solid rgba(255, 255, 255, .08);
  padding: 0;
  border-radius: 10px;
}

.table-wrap {
  overflow: auto;
}

.tbl {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.tbl th, .tbl td {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, .08);
  text-align: left;
}

.tbl thead th {
  position: sticky;
  top: 0;
  background: rgba(0, 0, 0, .15);
  z-index: 1;
}

.muted {
  opacity: .8;
  text-align: center;
}

.btn {
  border: 1px solid rgba(255, 255, 255, .2);
  background: rgba(255, 255, 255, .08);
  color: var(--text);
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
}

.btn:hover {
  background: rgba(255, 255, 255, .14);
}

.badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, .08);
  font-size: 12px;
}

.st-posted {
  background: rgba(76, 175, 80, .2);
}

.st-cancelled {
  background: rgba(244, 67, 54, .2);
}

.pager {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.actions {
  text-align: right;
}

/* Mobile cards (hidden on desktop) */
.mobile-list {
  display: none;
}

.mcard {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, .08);
}

.mcard__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 3px 0;
}

.mcard__id {
  font-weight: 700;
}

.mcard__lbl {
  color: #cfd8dc;
  font-size: 12px;
}

.mcard__val {
  font-weight: 600;
}

.mcard__actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 6px;
}

@media (max-width: 360px) {
  .desktop-only {
    display: none;
  }

  .mobile-list {
    display: block;
  }

  .btn {
    min-height: 40px;
  }
}
</style>
