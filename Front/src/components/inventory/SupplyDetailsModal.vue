<template>
  <transition name="fade-scale">
    <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-card">
        <div class="modal-head">
          <h3>Поставка #{{ supply?.id }}</h3>
        </div>
        <div class="modal-body">
          <div class="grid">
            <div>
              <div class="row"><span class="lbl">Склад</span><span class="val">{{
                  supply?.warehouseName || supply?.warehouse?.name || supply?.warehouseId || '—'
                }}</span></div>
              <div class="row"><span class="lbl">Поставщик</span><span class="val">{{
                  supplierName(supply?.supplierId)
                }}</span></div>
              <div class="row"><span class="lbl">Дата</span><span class="val">{{ fmtDate(supply?.date) }}</span></div>
            </div>
            <div>
              <div class="row"><span class="lbl">Статус</span><span class="val"><span :class="`st-${(supply?.status||'').toLowerCase()}`"
                                                                                      class="badge">{{
                  supply?.status || 'DRAFT'
                }}</span></span></div>
              <div class="row"><span class="lbl">Создал</span><span class="val">{{ createdByText }}</span></div>
              <div class="row"><span class="lbl">Заметки</span><span class="val">{{ supply?.notes || '—' }}</span></div>
            </div>
          </div>

          <h4 class="sub">Позиции ({{ Array.isArray(supply?.items) ? supply.items.length : 0 }})</h4>
          <div class="table-wrap">
            <table class="tbl">
              <thead>
              <tr>
                <th>#</th>
                <th>Ингредиент</th>
                <th>Кол-во</th>
                <th>Срок годности</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="(it, idx) in (supply?.items || [])" :key="idx">
                <td>{{ idx + 1 }}</td>
                <td>{{ it.ingredientName || it.ingredient?.name || it.ingredientId }}</td>
                <td>{{ it.qty }}</td>
                <td>{{ it.expiresAt || '—' }}</td>
              </tr>
              <tr v-if="!supply?.items || supply.items.length === 0">
                <td class="muted" colspan="4">Нет позиций</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn" type="button" @click="$emit('close')">Закрыть</button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import {computed} from 'vue';

const props = defineProps({
  visible: {type: Boolean, default: false},
  supply: {type: Object, default: null},
  suppliers: {type: Array, default: () => []},
});

const createdByText = computed(() => {
  // Бэкенд пока не отдаёт createdBy. Оставим прочерк.
  return '—';
});

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
  const m = (props.suppliers || []).find(x => String(x.id) === String(id));
  return m ? m.name : `#${id}`;
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 5000;
}

.modal-card {
  width: min(720px, 95vw);
  background: #1f1f1f;
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 12px;
  overflow: hidden;
  color: var(--text);
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.modal-head {
  padding: 12px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, .08);
  flex: 0 0 auto;
}

.modal-body {
  padding: 14px;
  display: grid;
  gap: 12px;
  overflow: auto;
  flex: 1 1 auto;
}

.modal-foot {
  padding: 12px 14px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid rgba(255, 255, 255, .08);
  flex: 0 0 auto;
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 4px 0;
}

.lbl {
  color: #cfd8dc;
  font-size: 13px;
}

.val {
  font-weight: 600;
}

.sub {
  margin: 4px 0;
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
  padding: 8px 10px;
  border-bottom: 1px solid rgba(255, 255, 255, .08);
  text-align: left;
}

.muted {
  color: #bdbdbd;
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

.fade-scale-enter-active, .fade-scale-leave-active {
  transition: opacity .18s ease, transform .18s ease;
}

.fade-scale-enter-from, .fade-scale-leave-to {
  opacity: 0;
  transform: scale(.98);
}

/* Mobile adaptations: iPhone SE (<=360px) */
@media (max-width: 360px) {
  .modal-card {
    width: 96vw;
  }

  .grid {
    grid-template-columns: 1fr;
  }

  .btn {
    min-height: 40px;
  }
}
</style>
