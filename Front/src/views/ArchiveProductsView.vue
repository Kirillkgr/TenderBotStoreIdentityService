<template>
  <div class="archive-page container py-3">
    <h1 class="mb-3">Архив товаров</h1>

    <div class="toolbar d-flex gap-2 align-items-end mb-3 flex-wrap">
      <div>
        <label class="form-label">Бренд</label>
        <select v-model.number="brandId" class="form-select">
          <option :value="0" disabled>— выберите бренд —</option>
          <option v-for="b in brands" :key="b.id" :value="b.id">{{ b.name }}</option>
        </select>
      </div>
      <div class="ms-auto d-flex gap-2">
        <button class="btn btn-outline-secondary" :disabled="!brandId || loading" @click="loadArchive">
          Обновить
        </button>
        <button class="btn btn-outline-danger" :disabled="!brandId || loading || archived.length===0" @click="purge">
          Очистить старше 90 дней
        </button>
      </div>
    </div>

    <div v-if="loading" class="text-muted">Загрузка...</div>
    <div v-else>
      <div v-if="!brandId" class="alert alert-info">Выберите бренд, чтобы просмотреть архив.</div>
      <div v-else-if="archived.length === 0" class="alert alert-secondary">Архив пуст.</div>
      <div v-else class="table-responsive">
        <table class="table table-sm align-middle">
          <thead>
          <tr>
            <th>ID</th>
            <th>Название</th>
            <th>Цена</th>
            <th>Промо</th>
            <th>Группа</th>
            <th>Архивирован</th>
            <th class="text-end">Действия</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="a in archived" :key="a.id">
            <td>{{ a.id }}</td>
            <td>{{ a.name }}</td>
            <td>{{ formatPrice(a.price) }}</td>
            <td>{{ a.promoPrice ? formatPrice(a.promoPrice) : '—' }}</td>
            <td>{{ a.groupTagId ?? 'корень' }}</td>
            <td>{{ formatDate(a.archivedAt) }}</td>
            <td class="text-end">
              <div class="btn-group btn-group-sm">
                <button class="btn btn-outline-primary" @click="restore(a)">Восстановить</button>
                <button class="btn btn-outline-danger" @click="remove(a)">Удалить</button>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue';
import {useProductStore} from '@/store/product';
import {getBrands} from '@/services/brandService';
import {useToast} from 'vue-toastification';

const toast = useToast();
const productStore = useProductStore();

const brands = ref([]);
const brandId = ref(0);
const archived = ref([]);
const loading = ref(false);

async function loadBrands() {
  try {
    const res = await getBrands();
    brands.value = Array.isArray(res) ? res : (res?.data ?? res?.data?.data ?? []);
    if (brands.value.length > 0 && !brandId.value) brandId.value = brands.value[0].id;
  } catch (e) {
    toast.error(e?.message || 'Не удалось загрузить бренды');
  }
}

async function loadArchive() {
  if (!brandId.value) return;
  loading.value = true;
  try {
    const res = await productStore.getArchiveByBrand(Number(brandId.value));
    archived.value = Array.isArray(res) ? res : (res?.data ?? res?.data?.data ?? []);
  } catch (e) {
    toast.error(e?.message || 'Не удалось загрузить архив');
  } finally {
    loading.value = false;
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

onMounted(async () => {
  await loadBrands();
  await loadArchive();
});
</script>

<style scoped>
.container { max-width: 1024px; }
.toolbar .form-select { min-width: 240px; }
.table td, .table th { vertical-align: middle; }
</style>
