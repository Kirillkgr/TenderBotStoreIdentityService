<template>
  <div class="archive-page py-3">
    <div class="archive-wrapper">
      <h1 class="mb-3 text-center">Архив товаров</h1>

      <div class="filters-bar mb-3">
        <div class="filters-left">
          <div class="form-group">
            <label class="form-label">Бренд</label>
            <select v-model.number="brandId" class="form-select">
              <option :value="0" disabled>— выберите бренд —</option>
              <option v-for="b in brands" :key="b.id" :value="b.id">{{ b.name }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Тип</label>
            <select v-model="typeFilter" class="form-select">
              <option value="all">Все</option>
              <option value="product">Товары</option>
              <option value="group">Теги</option>
            </select>
          </div>
          <div class="form-group form-search">
            <label class="form-label">Поиск</label>
            <input v-model.trim="q" class="form-control" placeholder="Имя, путь, тип..." type="text"/>
          </div>
        </div>
        <div class="filters-right">
          <button :disabled="!brandId || loadingAny" class="btn btn-outline-secondary" @click="refreshAll">Обновить
          </button>
          <button :disabled="!brandId || loadingAny || rows.length===0" class="btn btn-outline-danger" @click="purge">
            Очистить
          </button>
        </div>
      </div>

      <!-- Skeleton while loading (any) -->
      <div v-if="loadingAny" class="table-responsive archive-table-wrap">
        <table class="table table-sm align-middle archive-table table-bordered border-secondary">
          <thead>
          <tr>
            <th>№</th>
            <th>Имя</th>
            <th>Тип</th>
            <th>Путь</th>
            <th>Цена</th>
            <th>Промо</th>
            <th>Создан</th>
            <th>Изменён</th>
            <th>Удалён/Архивирован</th>
            <th class="text-end">Действия</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="i in 5" :key="i">
            <td>
              <div class="sk sk-text" style="width:34px"></div>
            </td>
            <td>
              <div class="sk sk-text" style="width:220px"></div>
            </td>
            <td>
              <div class="sk sk-text" style="width:60px"></div>
            </td>
            <td>
              <div class="sk sk-text" style="width:260px"></div>
            </td>
            <td>
              <div class="sk sk-text" style="width:70px"></div>
            </td>
            <td>
              <div class="sk sk-text" style="width:70px"></div>
            </td>
            <td>
              <div class="sk sk-text" style="width:140px"></div>
            </td>
            <td>
              <div class="sk sk-text" style="width:140px"></div>
            </td>
            <td>
              <div class="sk sk-text" style="width:160px"></div>
            </td>
            <td class="text-end">
              <div class="sk sk-btn" style="width:160px;height:28px;margin-left:auto"></div>
            </td>
          </tr>
          </tbody>
        </table>
        <!-- Pagination -->
        <div class="d-flex justify-content-between align-items-center mt-2">
          <div class="text-muted small">Показано {{ shownCount }} из {{ totalCount }}</div>
          <div class="pagination-wrap">
            <button :disabled="page===1" class="btn btn-sm btn-outline-secondary" @click="page=1">«</button>
            <button :disabled="page===1" class="btn btn-sm btn-outline-secondary" @click="page--">‹</button>
            <span class="mx-2">Стр. {{ page }} / {{ totalPagesUnified }}</span>
            <button :disabled="page===totalPages" class="btn btn-sm btn-outline-secondary" @click="page++">›</button>
            <button :disabled="page===totalPages" class="btn btn-sm btn-outline-secondary" @click="page=totalPages">»
            </button>
          </div>
        </div>
      </div>
      <div v-else class="table-responsive archive-table-wrap">
        <table class="table table-sm align-middle archive-table table-bordered border-secondary">
          <thead>
          <tr>
            <th>№</th>
            <th>Имя</th>
            <th>Тип</th>
            <th>Путь</th>
            <th>Цена</th>
            <th>Промо</th>
            <th>Создан</th>
            <th>Изменён</th>
            <th>Удалён/Архивирован</th>
            <th class="text-end">Действия</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(r, idx) in displayRows" :key="r._key">
            <td>{{ (page - 1) * pageSize + idx + 1 }}</td>
            <td>{{ r.name }}</td>
            <td>{{ r.type }}</td>
            <td class="path-cell">
              <span v-if="!isPathExpanded(r._key) && (r.path && r.path.length > 6)"
                    :title="r.path" class="path-ellipsis" @click="togglePath(r._key)">…</span>
              <span v-else :title="r.path" class="path-full">{{ r.path || '—' }}</span>
            </td>
            <td>{{ r.price }}</td>
            <td>{{ r.promoPrice }}</td>
            <td>{{ r.createdAt }}</td>
            <td>{{ r.updatedAt }}</td>
            <td>{{ r.archivedAt }}</td>
            <td class="text-end">
              <div class="action-wrap">
                <button v-if="r._kind==='product'" class="btn btn-sm btn-primary" @click="restoreByRow(r)">
                  Восстановить
                </button>
                <button v-if="r._kind==='product'" class="btn btn-sm btn-danger" @click="removeByRow(r)">Удалить
                </button>
                <button v-if="r._kind==='group'" class="btn btn-sm btn-primary" @click="restoreGroupRow(r)">
                  Восстановить
                </button>
                <button v-if="r._kind==='group'" class="btn btn-sm btn-danger" @click="removeGroupRow(r)">Удалить
                </button>
              </div>
            </td>
          </tr>
          </tbody>
          <tbody v-if="!displayRows.length">
          <tr>
            <td class="text-center text-muted" colspan="10">Архив пуст</td>
          </tr>
          </tbody>
        </table>
        <!-- Pagination -->
        <div class="d-flex justify-content-between align-items-center mt-2">
          <div class="text-muted small">Показано {{ shownCount }} из {{ totalCount }}</div>
          <div class="pagination-wrap">
            <button :disabled="page===1" class="btn btn-sm btn-outline-secondary" @click="page=1">«</button>
            <button :disabled="page===1" class="btn btn-sm btn-outline-secondary" @click="page--">‹</button>
            <span class="mx-2">Стр. {{ page }} / {{ totalPagesUnified }}</span>
            <button :disabled="page===totalPagesUnified" class="btn btn-sm btn-outline-secondary" @click="page++">›
            </button>
            <button :disabled="page===totalPagesUnified" class="btn btn-sm btn-outline-secondary"
                    @click="page=totalPagesUnified">»
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Плавающая кнопка назад в админку -->
    <button aria-label="Назад в админку" class="fab-back" title="Назад в админку" @click="goBackToAdmin">
      <svg aria-hidden="true" focusable="false" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <g id="Layer_50" data-name="Layer 50">
          <path
              d="M30,29a1,1,0,0,1-.81-.41l-2.12-2.92A18.66,18.66,0,0,0,15,18.25V22a1,1,0,0,1-1.6.8l-12-9a1,1,0,0,1,0-1.6l12-9A1,1,0,0,1,15,4V8.24A19,19,0,0,1,31,27v1a1,1,0,0,1-.69.95A1.12,1.12,0,0,1,30,29ZM14,16.11h.1A20.68,20.68,0,0,1,28.69,24.5l.16.21a17,17,0,0,0-15-14.6,1,1,0,0,1-.89-1V6L3.67,13,13,20V17.11a1,1,0,0,1,.33-.74A1,1,0,0,1,14,16.11Z"/>
        </g>
      </svg>
    </button>
  </div>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useRouter} from 'vue-router';
import {useProductStore} from '@/store/product';
import {getBrands} from '@/services/brandService';
import {useToast} from 'vue-toastification';
import tagService from '@/services/tagService';
import {formatLocalDateTime, parseServerDate} from '@/utils/datetime';

const toast = useToast();
const productStore = useProductStore();
const router = useRouter();

const brands = ref([]);
const brandId = ref(0);
const archived = ref([]);
const loading = ref(false);
const archivedTags = ref([]);
const loadingTags = ref(false);
const loadingAny = ref(false);

// Локальное переключение темы удалено — используется глобальная тема (theme-light/theme-dark)

// UX state
const q = ref('');
const typeFilter = ref('all'); // all | product | group
const page = ref(1);
const pageSize = ref(25);

// Server-side pagination state
const productPage = ref({content: [], totalElements: 0, totalPages: 1});
const tagPage = ref({content: [], totalElements: 0, totalPages: 1});

// Локальное состояние разворота длинных путей по ключу строки
const expandedPaths = ref(new Set());
const isPathExpanded = (key) => expandedPaths.value.has(key);

function togglePath(key) {
  if (expandedPaths.value.has(key)) expandedPaths.value.delete(key);
  else expandedPaths.value.add(key);
  // триггерим реактивность
  expandedPaths.value = new Set(expandedPaths.value);
}

async function loadBrands() {
  try {
    const res = await getBrands();
    brands.value = Array.isArray(res) ? res : (res?.data ?? res?.data?.data ?? []);
    if (brands.value.length > 0 && !brandId.value) brandId.value = brands.value[0].id;
  } catch (e) {
    toast.error(e?.message || 'Не удалось загрузить бренды');
  }

}

// Unified rows for table
const rows = computed(() => {
  const prodRows = (archived.value || []).map(a => ({
    _key: `p-${a.id}`,
    _kind: 'product',
    id: a.id,
    name: safe(a, 'name'),
    type: 'товар',
    path: computePath(a),
    price: formatPrice(a.price),
    promoPrice: a.promoPrice ? formatPrice(a.promoPrice) : '—',
    createdAt: formatDate(a.createdAt),
    updatedAt: formatDate(a.updatedAt),
    archivedAt: formatDate(a.archivedAt),
    _sortArchivedAt: (() => {
      const d = parseServerDate(a.archivedAt);
      return d ? d.getTime() : 0;
    })(),
    _sortUpdatedAt: (() => {
      const d = parseServerDate(a.updatedAt);
      return d ? d.getTime() : 0;
    })(),
  }));
  const tagRows = (archivedTags.value || []).map(g => ({
    _key: `g-${g.id}`,
    _kind: 'group',
    id: g.id,
    name: g.name,
    type: 'тег',
    path: g.path || '—',
    price: '—',
    promoPrice: '—',
    createdAt: '—',
    updatedAt: '—',
    archivedAt: formatDate(g.archivedAt),
    _sortArchivedAt: (() => {
      const d = parseServerDate(g.archivedAt);
      return d ? d.getTime() : 0;
    })(),
    _sortUpdatedAt: 0,
  }));
  return [...prodRows, ...tagRows]
      .sort((a, b) => (b._sortArchivedAt - a._sortArchivedAt) || (b._sortUpdatedAt - a._sortUpdatedAt));
});

const filteredRows = computed(() => {
  const qv = q.value.toLowerCase();
  return rows.value.filter(r => {
    if (typeFilter.value !== 'all' && r._kind !== typeFilter.value) return false;
    if (!qv) return true;
    return (
        String(r.id).includes(qv) ||
        (r.name || '').toLowerCase().includes(qv) ||
        (r.type || '').toLowerCase().includes(qv) ||
        (r.path || '').toLowerCase().includes(qv)
    );
  });
});

const totalPages = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / pageSize.value)));
const pagedRows = computed(() => {
  if (page.value > totalPages.value) page.value = totalPages.value;
  const start = (page.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

// Server-mode switch: when not 'all', use server pagination
const isServerMode = computed(() => typeFilter.value !== 'all');

const displayRows = computed(() => {
  if (!isServerMode.value) return pagedRows.value;
  if (typeFilter.value === 'product') {
    return (productPage.value.content || []).map(a => ({
      _key: `p-${a.id}`,
      _kind: 'product',
      id: a.id,
      name: safe(a, 'name'),
      type: 'товар',
      path: computePath(a),
      price: formatPrice(a.price),
      promoPrice: a.promoPrice ? formatPrice(a.promoPrice) : '—',
      createdAt: formatDate(a.createdAt),
      updatedAt: formatDate(a.updatedAt),
      archivedAt: formatDate(a.archivedAt),
    }));
  } else {
    return (tagPage.value.content || []).map(g => ({
      _key: `g-${g.id}`,
      _kind: 'group',
      id: g.id,
      name: g.name,
      type: 'тег',
      path: g.path || '—',
      price: '—',
      promoPrice: '—',
      createdAt: '—',
      updatedAt: '—',
      archivedAt: formatDate(g.archivedAt),
    }));
  }
});

const totalCount = computed(() => {
  if (!isServerMode.value) return filteredRows.value.length;
  return typeFilter.value === 'product' ? (productPage.value.totalElements || 0) : (tagPage.value.totalElements || 0);
});

const totalPagesServer = computed(() => {
  return typeFilter.value === 'product' ? (productPage.value.totalPages || 1) : (tagPage.value.totalPages || 1);
});

const totalPagesUnified = computed(() => isServerMode.value ? totalPagesServer.value : totalPages.value);
const shownCount = computed(() => displayRows.value.length);

// expose unified totalPages used in template
const totalPagesRef = computed(() => totalPagesUnified.value);

function restoreByRow(r) {
  if (r._kind === 'product') return restore({id: r.id, name: r.name});
}

function removeByRow(r) {
  if (r._kind === 'product') return remove({id: r.id});
}

async function restoreGroupRow(r) {
  try {
    await tagService.restoreGroupFromArchive(r.id);
    toast.success(`Тег «${r.name}» восстановлен`);
    await loadTags();
  } catch (e) {
    toast.error(e?.message || 'Не удалось восстановить тег');
  }
}

async function removeGroupRow(r) {
  try {
    await tagService.deleteGroupArchive(r.id);
    toast.success('Запись архива тега удалена');
    await loadTags();
  } catch (e) {
    toast.error(e?.message || 'Не удалось удалить запись архива тега');
  }
}

async function loadArchive() {
  if (!brandId.value) return;
  loading.value = true;
  loadingAny.value = true;
  try {
    const res = await productStore.getArchiveByBrand(Number(brandId.value));
    archived.value = Array.isArray(res) ? res : (res?.data ?? res?.data?.data ?? []);
  } catch (e) {
    toast.error(e?.message || 'Не удалось загрузить архив');
  } finally {
    loading.value = false;
    loadingAny.value = loadingTags.value;
  }
}

async function loadTags() {
  if (!brandId.value) return;
  loadingTags.value = true;
  loadingAny.value = true;
  try {
    const res = await tagService.getGroupArchiveByBrand(Number(brandId.value));
    archivedTags.value = Array.isArray(res) ? res : (res?.data ?? res?.data?.data ?? []);
  } catch (e) {
    toast.error(e?.message || 'Не удалось загрузить архив тегов');
  } finally {
    loadingTags.value = false;
    loadingAny.value = loading.value;
  }
}

async function restore(item) {
  try {
    await productStore.restoreFromArchive(item.id);
    toast.success(`Восстановлен «${item.name}»`);
    await loadArchive();
  } catch (e) {
    toast.error(e?.message || 'Не удалось восстановить');
  }
}

async function remove(item) {
  try {
    await productStore.deleteArchived(item.id);
    toast.success('Запись удалена из архива');
    await loadArchive();
  } catch (e) {
    toast.error(e?.message || 'Не удалось удалить запись');
  }
}

async function purge() {
  try {
    await productStore.purgeArchive(90);
    toast.success('Архив очищен (старше 90 дней)');
    await loadArchive();
  } catch (e) {
    toast.error(e?.message || 'Не удалось очистить архив');
  }
}

function formatPrice(val) {
  if (val == null || val === '') return '—';
  const n = Number(val); return Number.isNaN(n) ? String(val) : n.toFixed(2);
}
function formatDate(val) {
  try {
    return formatLocalDateTime(val);
  } catch {
    return String(val || '—');
  }
}

function safe(obj, key) {
  try {
    return obj?.[key] ?? '—';
  } catch {
    return '—';
  }
}

function computePath(item) {
  // пытаемся взять один из возможных путей, иначе собрать из бренда+группы
  const p = item?.path || item?.groupPath || item?.fullPath;
  if (p) return p;
  const brand = item?.brandName || brands.value.find(b => b.id === item?.brandId)?.name;
  const group = item?.groupTagName ?? item?.groupTagId;
  let parts = [];
  if (brand) parts.push(brand);
  if (group) parts.push(String(group));
  return parts.length ? `/${parts.join('/')}/` : '—';
}

onMounted(async () => {
  await loadBrands();
  // Дальше загрузка архива делается через watch(brandId)
  // Тема управляется глобально (см. AppHeader/main.js)
});

watch(brandId, async (v, old) => {
  if (v && v !== old) {
    if (typeFilter.value === 'all') {
      await Promise.all([loadArchive(), loadTags()]);
    } else if (typeFilter.value === 'product') {
      await loadProductPage();
    } else {
      await loadTagPage();
    }
    page.value = 1;
  }
});

watch([typeFilter, page, pageSize], async () => {
  if (!brandId.value) return;
  if (typeFilter.value === 'all') {
    // Локальная пагинация — подгружаем полные списки
    await Promise.all([loadArchive(), loadTags()]);
  } else if (typeFilter.value === 'product') {
    await loadProductPage();
  } else if (typeFilter.value === 'group') {
    await loadTagPage();
  }
});

// Floating back button handler
function goBackToAdmin() {
  try {
    router.push({name: 'Admin'});
  } catch (_) {
    router.push('/admin');
  }
}

// Обновить данные в зависимости от текущего режима
function refreshAll() {
  page.value = 1;
  if (typeFilter.value === 'all') {
    return Promise.all([loadArchive(), loadTags()]);
  } else if (typeFilter.value === 'product') {
    return loadProductPage();
  } else {
    return loadTagPage();
  }
}

</script>

<style scoped>
.archive-wrapper {
  max-width: 980px;
  margin: 0 auto;
  padding: 0 12px;
}

.table td, .table th { vertical-align: middle; }

/* Archive table UX */
.archive-table-wrap {
  margin: 0 auto;
  max-width: 980px;
}

.archive-table {
  border-collapse: separate;
  border-spacing: 0;
  background-color: var(--table-bg);
  color: var(--table-text);
}

.archive-table thead th {
  position: sticky;
  top: 0;
  background-color: var(--header-bg);
  color: var(--header-text);
  z-index: 2;
  box-shadow: inset 0 -1px 0 var(--border);
}

.archive-table tbody tr {
  background-color: var(--table-bg);
}

.archive-table tbody tr:nth-child(even) {
  background-color: var(--row-alt);
}

.archive-table tbody tr:hover {
  background: var(--row-hover);
}

.archive-table td, .archive-table th {
  padding-top: 0.6rem;
  padding-bottom: 0.6rem;
  border-color: var(--border) !important;
}

.archive-table {
  border-radius: 6px;
  overflow: hidden;
  box-shadow: none;
}

.action-wrap {
  display: inline-flex;
  gap: 8px;
  justify-content: flex-end;
}

.pagination-wrap .btn {
  min-width: 36px;
}

/* Фильтры в одну строку */
.filters-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: space-between;
  flex-wrap: wrap;
}

.filters-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filters-left .form-group {
  min-width: 210px;
}

.filters-left .form-group.form-search {
  min-width: 260px;
  flex: 1 1 280px;
}

.filters-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* Скрываем подписи у контролов в панели фильтров, используем плейсхолдеры */
.filters-bar .form-label {
  display: none;
}

@media (max-width: 768px) {
  .filters-left .form-group {
    min-width: 160px;
  }

  .filters-left .form-group.form-search {
    min-width: 200px;
    flex: 1 1 200px;
  }

  .archive-wrapper, .archive-table-wrap {
    max-width: 100%;
  }
}

/* Skeleton styles */
.sk {
  position: relative;
  overflow: hidden;
  background: #3a3a3a;
  border-radius: 6px;
}

.sk::after {
  content: '';
  position: absolute;
  inset: 0;
  transform: translateX(-100%);
  background: linear-gradient(90deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0));
  animation: sk-shine 1.2s infinite;
}

.sk-text {
  height: 14px;
}

.sk-btn {
  height: 28px;
  border-radius: 14px;
}

@keyframes sk-shine {
  100% {
    transform: translateX(100%);
  }
}

.archive-table.table-bordered, .archive-table.table-bordered > :not(caption) > * {
  border-color: var(--border) !important;
}

/* Явные вертикальные разделители */
.archive-table thead th, .archive-table tbody td {
  border-right: 1px solid var(--border) !important;
}

.archive-table thead th:last-child, .archive-table tbody td:last-child {
  border-right: 0 !important;
}

/* Цвета кнопок действий (используют глобальную палитру) */
.btn.btn-primary {
  background-color: #0d6efd;
  border-color: #0d6efd;
}

.path-cell {
  max-width: 260px;
}

.path-ellipsis {
  cursor: pointer;
  display: inline-block;
  padding: 0 6px;
  border-radius: 6px;
  background: var(--input-bg);
  border: 1px solid var(--border);
}

.path-ellipsis:hover {
  background: var(--input-bg-hover);
}

.path-full {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  max-width: 260px;
}
</style>

<style scoped>
/* Floating Back Button styling */
.fab-back {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background: #2563eb; /* blue */
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.3);
  cursor: pointer;
  z-index: 1002;
  transition: transform .15s ease, box-shadow .15s ease, background .15s ease;
}

.fab-back:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.36);
  background: #1d4ed8;
}

.fab-back:active {
  transform: translateY(0);
}

.fab-back svg {
  width: 24px;
  height: 24px;
}

.fab-back svg path {
  fill: currentColor;
}

@media (max-width: 480px) {
  .fab-back {
    right: 16px;
    bottom: 16px;
    width: 52px;
    height: 52px;
  }
}
</style>
