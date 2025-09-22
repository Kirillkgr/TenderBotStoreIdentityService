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
          <div class="text-muted small">Показано {{ pagedRows.length }} из {{ filteredRows.length }}</div>
          <div class="pagination-wrap">
            <button :disabled="page===1" class="btn btn-sm btn-outline-secondary" @click="page=1">«</button>
            <button :disabled="page===1" class="btn btn-sm btn-outline-secondary" @click="page--">‹</button>
            <span class="mx-2">Стр. {{ page }} / {{ totalPages }}</span>
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
          <tr v-for="(r, idx) in pagedRows" :key="r._key">
            <td>{{ (page - 1) * pageSize + idx + 1 }}</td>
            <td>{{ r.name }}</td>
            <td>{{ r.type }}</td>
            <td :title="r.path" class="text-truncate" style="max-width:260px">{{ r.path }}</td>
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
          <tbody v-if="!pagedRows.length">
          <tr>
            <td class="text-center text-muted" colspan="10">Архив пуст</td>
          </tr>
          </tbody>
        </table>
        <!-- Pagination -->
        <div class="d-flex justify-content-between align-items-center mt-2">
          <div class="text-muted small">Показано {{ pagedRows.length }} из {{ filteredRows.length }}</div>
          <div class="pagination-wrap">
            <button :disabled="page===1" class="btn btn-sm btn-outline-secondary" @click="page=1">«</button>
            <button :disabled="page===1" class="btn btn-sm btn-outline-secondary" @click="page--">‹</button>
            <span class="mx-2">Стр. {{ page }} / {{ totalPages }}</span>
            <button :disabled="page===totalPages" class="btn btn-sm btn-outline-secondary" @click="page++">›</button>
            <button :disabled="page===totalPages" class="btn btn-sm btn-outline-secondary" @click="page=totalPages">»
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useProductStore} from '@/store/product';
import {getBrands} from '@/services/brandService';
import {useToast} from 'vue-toastification';
import tagService from '@/services/tagService';

const toast = useToast();
const productStore = useProductStore();

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
    _sortArchivedAt: a.archivedAt ? new Date(a.archivedAt).getTime() : 0,
    _sortUpdatedAt: a.updatedAt ? new Date(a.updatedAt).getTime() : 0,
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
    _sortArchivedAt: g.archivedAt ? new Date(g.archivedAt).getTime() : 0,
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
  try { return new Date(val).toLocaleString(); } catch { return String(val || '—'); }
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
    await Promise.all([loadArchive(), loadTags()]);
    page.value = 1;
  }
});

function refreshAll() {
  page.value = 1;
  return Promise.all([loadArchive(), loadTags()]);
}
</script>

<style scoped>
.archive-wrapper {
  max-width: 980px;
  margin: 0 auto;
  padding: 0 12px;
}
.toolbar .form-select { min-width: 240px; }
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

.theme-btn {
  white-space: nowrap;
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

</style>
