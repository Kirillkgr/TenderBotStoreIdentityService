<template>
  <section class="brands-page">
    <header class="page-header">
      <div class="actions">
        <button
            v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
            aria-label="Создать бренд"
            class="btn primary icon"
            title="Создать бренд"
            @click="onCreate"
        >+
        </button>
        <span aria-hidden="true" class="divider"></span>
        <input v-model="q" placeholder="Поиск по названию" type="text"/>
      </div>
    </header>

    <div v-if="loading" class="loading">Загрузка…</div>
    <div v-else>
      <div class="table-wrap">
        <table class="table">
          <thead>
          <tr>
            <th>#</th>
            <th>Название</th>
            <th>Юр. наименование</th>
            <th>Описание</th>
            <th>Telegram Bot Token</th>
            <th>Создан</th>
            <th>Изменён</th>
            <th style="width: 120px">Действия</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(b, idx) in filtered" :key="b.id">
            <td>{{ idx + 1 }}</td>
            <td class="name-cell">
              <span class="name-text" :title="b.name">{{ b.name }}</span>
              <button
                  v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
                  class="name-edit-btn"
                  title="Редактировать бренд"
                  @click="onEdit(b)"
              >✏️</button>
            </td>
            <td>{{ b.organizationName || '—' }}</td>
            <td :title="b.description || '—'">{{
                b.description ? (b.description.length > 40 ? (b.description.slice(0, 40) + '…') : b.description) : '—'
              }}
            </td>
            <td>
              <span :title="b.telegramBotToken || '—'">
                {{ b.telegramBotToken ? (b.telegramBotToken.slice(0, 6) + '…') : '—' }}
              </span>
            </td>
            <td>{{ b.createdAt ? b.createdAt.substring(0, 10) : '—' }}</td>
            <td>{{ b.updatedAt ? b.updatedAt.substring(0, 10) : '—' }}</td>
            <td class="row-actions">
              <button
                  class="btn"
                  title="Просмотр"
                  @click="onView(b)"
              >👁️
              </button>
              <button
                  v-can="{ any: ['ADMIN','OWNER'], mode: 'disable', tooltip: 'Недостаточно прав' }"
                  class="btn danger"
                  @click="onArchive(b)"
              >🗑️
              </button>
            </td>
          </tr>
          <tr v-if="filtered.length === 0">
            <td class="muted" colspan="5">Нет данных</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <EditBrandModal
        v-if="showEdit"
        :brand="editing"
        :mode="editMode"
        @close="onCancel"
        @saved="onSaved"
    />

    <ViewBrandModal
        v-if="showView"
        :brand="viewing"
        @close="onCloseView"
    />
  </section>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {deleteBrand, getBrands} from '@/services/brandService';
import EditBrandModal from '@/components/modals/EditBrandModal.vue';
import ViewBrandModal from '@/components/modals/ViewBrandModal.vue';

const loading = ref(false);
const items = ref([]);
const q = ref('');

const filtered = computed(() => {
  const query = q.value.trim().toLowerCase();
  return (items.value || []).filter(x => !query || (x.name || '').toLowerCase().includes(query));
});

const showEdit = ref(false);
const editing = ref(null);
const editMode = ref('edit'); // 'edit' | 'create'
const showView = ref(false);
const viewing = ref(null);

async function load() {
  try {
    loading.value = true;
    const resp = await getBrands();
    items.value = Array.isArray(resp?.data) ? resp.data : (resp?.data ?? []);
  } finally {
    loading.value = false;
  }
}

function onCreate() {
  editMode.value = 'create';
  editing.value = null;
  showEdit.value = true;
}

function onEdit(brand) {
  editMode.value = 'edit';
  editing.value = {...brand};
  showEdit.value = true;
}

function onCancel() {
  showEdit.value = false;
  editing.value = null;
}

async function onSaved(_) {
  showEdit.value = false;
  editing.value = null;
  await load();
}

function onView(brand) {
  viewing.value = brand;
  showView.value = true;
}

function onCloseView() {
  showView.value = false;
  viewing.value = null;
}

async function onArchive(brand) {
  if (!brand?.id) return;
  if (!confirm(`Отправить бренд "${brand.name}" в архив?`)) return;
  try {
    await deleteBrand(brand.id);
    await load();
  } catch (e) {
    alert(e?.response?.data?.message || 'Не удалось архивировать бренд');
  }
}

onMounted(load);
</script>

<style scoped>
.btn.icon {
  width: 34px;
  height: 34px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  line-height: 1;
  border-radius: 8px;
}

.divider {
  display: inline-block;
  width: 1px;
  height: 28px;
  background: #d1d5db;
  margin: 0 8px;
}

.brands-page {
  padding: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.table-wrap {
  overflow: hidden;
}

.table th, .table td {
  border-bottom: 1px solid #e5e7eb;
  padding: 8px;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.table td.row-actions {
  overflow: visible;
  text-overflow: clip;
}

.row-actions {
  display: flex;
  gap: 6px;
}

.btn {
  padding: 6px 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}

.btn.primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}

.btn.danger {
  background: #ef4444;
  color: #fff;
  border-color: #ef4444;
}

.loading {
  padding: 16px;
}

.muted {
  color: #6b7280;
  text-align: center;
}

input[type="text"] {
  padding: 6px 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
}

/* Hover edit button over brand name */
.name-cell {
  position: relative;
}

.name-text {
  display: inline-block;
  max-width: 100%;
  padding-right: 28px; /* space for edit button */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.name-edit-btn {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  pointer-events: none;
  border: 1px solid #d1d5db;
  background: #fff;
  border-radius: 6px;
  width: 24px;
  height: 24px;
  line-height: 22px;
  font-size: 14px;
  cursor: pointer;
}

.name-cell:hover .name-edit-btn {
  opacity: 1;
  pointer-events: auto;
}
</style>
