<template>
  <div class="modal-backdrop" @click.self="onCancel">
    <div class="modal">
      <header class="modal-header">
        <h3>Новая поставка</h3>
        <button class="icon" @click="onCancel">✕</button>
      </header>

      <section class="modal-body">
        <div class="grid two">
          <div>
            <label>Склад</label>
            <input :value="warehouseId" disabled/>
          </div>
          <div>
            <label>Дата поставки</label>
            <input v-model="form.date" type="datetime-local"/>
          </div>
        </div>

        <div class="grid two">
          <div>
            <label>Поставщик</label>
            <select v-model.number="form.supplierId">
              <option :value="0">— не указан —</option>
              <option v-for="s in suppliers" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>
          <div>
            <label>Заметки</label>
            <input v-model.trim="form.notes" maxlength="1024" placeholder="опционально"/>
          </div>
        </div>

        <div class="items">
          <div class="items-head">
            <div>Ингредиент</div>
            <div>Фасовка</div>
            <div>Кол-во фас.</div>
            <div>Итого (баз. ед.)</div>
            <div>Срок годности</div>
            <div></div>
          </div>
          <div v-for="(it, idx) in form.items" :key="idx" class="items-row">
            <div>
              <select v-model.number="it.ingredientId" @change="onIngredientChange(it)">
                <option :value="0" disabled>Выберите ингредиент</option>
                <option v-for="i in ingredients" :key="i.id" :value="i.id">{{ i.name }}</option>
              </select>
            </div>
            <div>
              <select v-model.number="it.packagingId" :disabled="!availablePackagings(it).length"
                      @change="recalcQty(it)">
                <option :value="0">— не выбрано —</option>
                <option v-for="p in availablePackagings(it)" :key="p.id" :value="p.id">
                  {{ p.name }} × {{ p.size }} {{ unitShort(unitIdOf(it)) }}
                </option>
              </select>
            </div>
            <div>
              <input v-model.number="it.packageCount" min="0" step="0.001" type="number" @input="recalcQty(it)"/>
            </div>
            <div>
              <div class="qty-wrap">
                <input v-model.number="it.qty" min="0" step="0.001" type="number"/>
                <span class="unit">{{ unitShort(unitIdOf(it)) }}</span>
              </div>
            </div>
            <div>
              <input v-model="it.expiresAt" type="date"/>
            </div>
            <div>
              <button class="icon" title="Удалить" @click="removeItem(idx)">🗑</button>
            </div>
          </div>
          <div class="items-actions">
            <button class="btn" @click="addItem">Добавить позицию</button>
          </div>
        </div>

        <p class="hint">Подсказка: при выборе фасовки итоговое количество рассчитывается автоматически. Его можно
          изменить вручную.</p>
      </section>

      <footer class="modal-footer">
        <button class="btn" @click="onCancel">Отмена</button>
        <button :disabled="!canSave" class="btn primary" @click="onSave">Сохранить и провести</button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import {computed, reactive} from 'vue';

const props = defineProps({
  warehouseId: {type: Number, required: true},
  ingredients: {type: Array, default: () => []},
  suppliers: {type: Array, default: () => []},
  units: {type: Array, default: () => []},
  packagings: {type: Array, default: () => []},
});

const emit = defineEmits(['cancel', 'save']);

const form = reactive({
  supplierId: 0,
  date: toLocalDateTimeInput(new Date()),
  notes: '',
  items: [emptyItem()],
});

function emptyItem() {
  return {ingredientId: 0, qty: 0, expiresAt: '', packagingId: 0, packageCount: 0};
}

function addItem() {
  form.items.push(emptyItem());
}

function removeItem(idx) {
  form.items.splice(idx, 1);
  if (form.items.length === 0) addItem();
}

function unitShort(unitId) {
  const u = (props.units || []).find(x => x.id === unitId);
  return u ? (u.shortName || u.name) : '—';
}

function unitIdOf(it) {
  const ing = (props.ingredients || []).find(x => x.id === it.ingredientId);
  return ing?.unitId || ing?.unit?.id || null;
}

function availablePackagings(it) {
  // Пока нет привязки фасовок к ингредиенту — показываем все, базовая ед. та же
  return props.packagings || [];
}

function onIngredientChange(it) {
  // Сброс фасовки при смене ингредиента
  it.packagingId = 0;
  it.packageCount = 0;
  recalcQty(it);
}

function recalcQty(it) {
  const p = (props.packagings || []).find(x => x.id === it.packagingId);
  const cnt = Number(it.packageCount || 0);
  if (p && cnt > 0) {
    const size = Number(p.size || 0);
    it.qty = round(size * cnt, 3);
  }
}

function round(v, d) {
  const k = Math.pow(10, d || 3);
  return Math.round((Number(v) || 0) * k) / k;
}

const canSave = computed(() => {
  if (!props.warehouseId) return false;
  if (!form.date) return false;
  if (!Array.isArray(form.items) || form.items.length === 0) return false;
  return form.items.every(i => i.ingredientId > 0 && Number(i.qty) > 0);
});

function onCancel() {
  emit('cancel');
}

function onSave() {
  emit('save', {
    warehouseId: props.warehouseId,
    supplierId: form.supplierId || null,
    date: new Date(form.date).toISOString(),
    notes: form.notes?.trim() || null,
    items: form.items.map(i => ({ingredientId: i.ingredientId, qty: Number(i.qty), expiresAt: i.expiresAt || null}))
  });
}

function toLocalDateTimeInput(d) {
  const pad = n => String(n).padStart(2, '0');
  const dt = new Date(d);
  const yyyy = dt.getFullYear();
  const mm = pad(dt.getMonth() + 1);
  const dd = pad(dt.getDate());
  const hh = pad(dt.getHours());
  const mi = pad(dt.getMinutes());
  return `${yyyy}-${mm}-${dd}T${hh}:${mi}`;
}
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.modal {
  width: min(980px, 96vw);
  background: #111827;
  color: #e5e7eb;
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 10px;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, .08);
}

.modal-body {
  padding: 12px 14px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 14px;
  border-top: 1px solid rgba(255, 255, 255, .08);
}

.grid.two {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.grid.two > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.items {
  margin-top: 12px;
}

.items-head {
  display: grid;
  grid-template-columns: 2.2fr 1.6fr 1.2fr 1.4fr 1.4fr 0.6fr;
  gap: 8px;
  padding: 6px 0;
  color: #9ca3af;
  font-size: 13px;
}

.items-row {
  display: grid;
  grid-template-columns: 2.2fr 1.6fr 1.2fr 1.4fr 1.4fr 0.6fr;
  gap: 8px;
  padding: 6px 0;
  align-items: center;
}

.qty-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
}

.unit {
  color: #9ca3af;
  font-size: 12px;
}

.btn {
  padding: 6px 10px;
  border: 1px solid #374151;
  border-radius: 6px;
  background: #111827;
  color: #e5e7eb;
  cursor: pointer;
}

.btn.primary {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}

.icon {
  border: none;
  background: transparent;
  cursor: pointer;
  color: #e5e7eb;
}

.hint {
  color: #9ca3af;
  margin-top: 6px;
  font-size: 12px;
}

select, input {
  padding: 6px 8px;
  border: 1px solid #374151;
  border-radius: 6px;
  background: #0b1220;
  color: #e5e7eb;
}
</style>
