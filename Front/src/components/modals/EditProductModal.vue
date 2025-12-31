<template>
  <div class="epm-overlay" @click.self="onClose">
    <div class="epm-card" role="dialog" aria-modal="true" :style="cardStyle">
      <header class="epm-header" @mousedown.prevent="onHeaderDown">
        <h3 class="epm-title">{{ modalTitle }}</h3>
        <button class="epm-close" aria-label="Закрыть" @click="onClose">×</button>
      </header>

      <section class="epm-content">
        <div class="epm-left">
          <div
            class="epm-dropzone"
            :class="{ 'is-dragover': isDragOver }"
            @dragover.prevent="isDragOver = true"
            @dragleave.prevent="isDragOver = false"
            @drop.prevent="onDrop"
          >
            <img v-if="previewUrl" :src="previewUrl" :alt="form.name" />
            <div v-else class="epm-placeholder" aria-label="Нет изображения">
              <img :src="PRODUCTION_SVG_DATA" alt="Нет изображения" class="epm-fallback-img" />
            </div>
            <div class="epm-dropzone-actions">
              <input ref="fileInput" type="file" accept="image/*" @change="onFileChange" hidden />
              <button class="btn btn-secondary" type="button" @click="() => fileInput?.click()">Загрузить</button>
            </div>
          </div>
        </div>

        <div class="epm-right">
          <div class="form-row two-cols">
            <div class="vis-row">
              <label>Видимость</label>
              <label class="switch">
                <input type="checkbox" v-model="form.visible" />
                <span class="slider"></span>
              </label>
            </div>
          </div>

          <div class="form-row">
            <label>Название</label>
            <input class="input" v-model.trim="form.name" type="text" placeholder="Название товара" />
            <p v-if="errors.name" class="error">{{ errors.name }}</p>
          </div>

          <div class="form-row two-cols">
            <div>
              <label>Цена</label>
              <input class="input" v-model.number="form.price" type="number" min="0" step="0.01" />
            </div>
            <div>
              <label>Промо-цена</label>
              <input class="input" v-model.number="form.promoPrice" type="number" min="0" step="0.01" />
            </div>
          </div>

          <div class="form-row">
            <label>Описание</label>
            <textarea class="textarea" v-model.trim="form.description" rows="4" placeholder="Краткое описание" />
          </div>

          <div class="form-row two-cols">
            <div>
              <label>Бренд</label>
              <select class="select" v-model.number="form.brandId">
                <option v-for="b in brands" :key="b.id" :value="b.id">{{ b.name }}</option>
              </select>
            </div>
            <div>
              <label>Группа</label>
              <select class="select" v-model.number="form.groupTagId">
                <option :value="0">Корень</option>
                <option v-for="g in groupSelectOptions" :key="g.id" :value="g.id">{{ g.label }}</option>
              </select>
            </div>
          </div>

          <div class="form-row">
            <label>Теги</label>
            <div class="multiselect">
              <div
                v-for="tag in tagOptions"
                :key="tag.id"
                class="chip"
                :class="{ selected: form.tagIds.includes(tag.id) }"
                @click="toggleTag(tag.id)"
                role="button"
                tabindex="0"
              >
                {{ tag.name }}
              </div>
            </div>
          </div>
        </div>
      </section>

      <footer class="epm-footer">
        <button class="btn btn-secondary" type="button" @click="onClose">Отмена</button>
        <button v-if="!isCreate" class="btn btn-secondary" type="button" @click="$emit('delete')" style="margin-right:auto;color:#b91c1c;border-color:#b91c1c;">Удалить</button>
        <button class="btn btn-primary" type="button" :disabled="saving" @click="onSave">
          <span v-if="saving" class="loader"></span>
          Сохранить
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import {computed, reactive, ref, watch} from 'vue';
import tagService from '@/services/tagService';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  product: { type: Object, required: true },
  brands: { type: Array, default: () => [] },
  groupOptions: { type: Array, default: () => [] }, // legacy, not used when tree available
  tagOptions: { type: Array, default: () => [] },   // [{id, name}]
  theme: { type: String, default: 'auto' },         // 'light' | 'dark' | 'auto'
  currentRootTagId: { type: [Number, String], default: 0 },
});

const emit = defineEmits(['update:modelValue', 'close', 'save', 'delete']);

const fileInput = ref(null);
const isDragOver = ref(false);
const saving = ref(false);

// Режим модалки: создание или редактирование
const isCreate = computed(() => !props.product || !props.product.id);
const modalTitle = computed(() => (isCreate.value ? 'Создание товара' : 'Редактирование товара'));

const form = reactive({
  id: props.product?.id,
  name: props.product?.name || '',
  price: props.product?.price ?? 0,
  promoPrice: props.product?.promoPrice ?? null,
  description: props.product?.description || '',
  brandId: props.product?.brandId || props.product?.brand?.id || null,
  groupTagId: props.product?.groupTagId ?? 0,
  tagIds: Array.isArray(props.product?.tagIds) ? [...props.product.tagIds] : [],
  visible: props.product?.visible ?? true,
  imageFile: null,
  imageUrl: props.product?.imageUrl || null,
});

watch(() => props.product, (p) => {
  if (!p) return;
  form.id = p.id;
  form.name = p.name || '';
  form.price = p.price ?? 0;
  form.promoPrice = p.promoPrice ?? null;
  form.description = p.description || '';
  form.brandId = p.brandId || p.brand?.id || null;
  form.groupTagId = p.groupTagId ?? 0;
  form.tagIds = Array.isArray(p.tagIds) ? [...p.tagIds] : [];
  form.imageUrl = p.imageUrl || null;
  form.imageFile = null;
  form.visible = p.visible ?? true;
}, { immediate: true });

// Фолбэки по умолчанию, если не пришёл brandId/группа
watch([() => form.brandId, () => props.brands], ([bid]) => {
  if ((bid === null || bid === undefined) && Array.isArray(props.brands) && props.brands.length > 0) {
    form.brandId = props.brands[0].id;
  }
}, { immediate: true });
watch(() => form.groupTagId, (gid) => {
  if (gid === null || gid === undefined) form.groupTagId = 0;
}, { immediate: true });

const errors = reactive({ name: '' });

// Тема берётся из глобальных CSS-переменных (theme.css) через классы на html

const previewUrl = computed(() => {
  if (form.imageFile) return URL.createObjectURL(form.imageFile);
  return form.imageUrl;
});

// Встроенный production.svg как data:URI
const PRODUCTION_SVG_DATA = 'data:image/svg+xml;utf8,' + encodeURIComponent(`<?xml version="1.0" encoding="UTF-8"?>
<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
  <defs>
    <style>.cls-1{fill:#231f20;}</style>
  </defs>
  <title>Wondicon - UI (Free)</title>
  <path class="cls-1" d="M100,175.69a5,5,0,0,1-2.23-.53L27.08,139.89a5,5,0,0,1-2.77-4.48V64.86a5,5,0,0,1,2.77-4.47L97.77,25.11a5,5,0,0,1,4.46,0l70.69,35.28a5,5,0,0,1,2.77,4.47v70.55a5,5,0,0,1-2.77,4.48l-70.69,35.27A5,5,0,0,1,100,175.69ZM34.31,132.32,100,165.1l65.69-32.78V68L100,35.17,34.31,68Zm136.38,3.09h0Z"/>
  <path class="cls-1" d="M100,105a5,5,0,0,1-2.23-.52L27.09,69.34a5,5,0,1,1,4.45-9l68.46,34,68.46-34a5,5,0,1,1,4.45,9l-70.68,35.14A5,5,0,0,1,100,105Z"/>
  <path class="cls-1" d="M135.34,87.43a5,5,0,0,1-2.22-.52L62.43,51.77a5,5,0,1,1,4.45-9L137.57,78a5,5,0,0,1-2.23,9.48Z"/>
  <path class="cls-1" d="M100,175.69a5,5,0,0,1-5-5V100a5,5,0,0,1,10,0v70.69A5,5,0,0,1,100,175.69Z"/>
</svg>`);

// Загрузка дерева тегов бренда и построение иерархии для выбора группы
const tagTree = ref([]);
const loadingTree = ref(false);

async function loadTagTree(brandId) {
  if (!brandId) { tagTree.value = []; return; }
  loadingTree.value = true;
  try {
    const tree = await tagService.getTagTree(Number(brandId));
    tagTree.value = Array.isArray(tree) ? tree : [];
  } catch (_) {
    tagTree.value = [];
  } finally {
    loadingTree.value = false;
  }
}

function findSubtree(items, rootId) {
  if (!rootId || rootId === 0) return items;
  const stack = [...(items || [])];
  while (stack.length) {
    const node = stack.shift();
    if (!node) continue;
    if (Number(node.id) === Number(rootId)) {
      return [node];
    }
    if (node.children && node.children.length) stack.push(...node.children);
  }
  return [];
}

function flatten(items, depth = 0) {
  return (items || []).flatMap(it => [
    { id: it.id, label: `${'— '.repeat(depth)}${it.name}` },
    ...(it.children?.length ? flatten(it.children, depth + 1) : [])
  ]);
}

const groupSelectOptions = computed(() => {
  const rootId = Number(props.currentRootTagId || 0);
  const base = findSubtree(tagTree.value, rootId);
  return flatten(base, rootId && rootId !== 0 ? 0 : 0);
});

watch(() => form.brandId, async (bid) => { await loadTagTree(bid); }, { immediate: true });

function toggleTag(id) {
  const idx = form.tagIds.indexOf(id);
  if (idx >= 0) form.tagIds.splice(idx, 1);
  else form.tagIds.push(id);
}

function onFileChange(e) {
  const file = e.target.files?.[0];
  if (file) form.imageFile = file;
}

function onDrop(e) {
  isDragOver.value = false;
  const file = e.dataTransfer?.files?.[0];
  if (file && file.type.startsWith('image/')) {
    form.imageFile = file;
  }
}

function onClose() {
  emit('update:modelValue', false);
  emit('close');
}

function validate() {
  errors.name = form.name ? '' : 'Введите название';
  if (form.promoPrice != null && form.promoPrice !== '' && Number(form.promoPrice) > Number(form.price)) {
    // мягкая проверка: промо не должна быть выше цены
    // не блокируем, но можем подсветить, если потребуется
  }
  return !errors.name;
}

async function onSave() {
  if (!validate()) return;
  saving.value = true;
  try {
    // Ничего не шлём тут — только эмитим наверх готовые данные.
    // Родитель решит, какое API вызвать (PUT/PATCH/загрузка файла и т.п.).
    const payload = { ...form };
    emit('save', payload);
  } finally {
    saving.value = false;
  }
}

// ====== Перетаскивание (drag) по заголовку ======
const drag = reactive({ dx: 0, dy: 0, startX: 0, startY: 0, dragging: false });

function onHeaderDown(e) {
  drag.dragging = true;
  drag.startX = e.clientX;
  drag.startY = e.clientY;
  window.addEventListener('mousemove', onDragMove);
  window.addEventListener('mouseup', onDragUp);
}

function onDragMove(e) {
  if (!drag.dragging) return;
  drag.dx += (e.clientX - drag.startX);
  drag.dy += (e.clientY - drag.startY);
  drag.startX = e.clientX;
  drag.startY = e.clientY;
}

function onDragUp() {
  drag.dragging = false;
  window.removeEventListener('mousemove', onDragMove);
  window.removeEventListener('mouseup', onDragUp);
}

const cardStyle = computed(() => ({
  position: 'fixed',
  top: '50%',
  left: '50%',
  transform: `translate(calc(-50% + ${drag.dx}px), calc(-50% + ${drag.dy}px))`,
}));
</script>

<style scoped>
/* Overlay */
.epm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fade-in 180ms ease;
}

/* Card */
.epm-card {
  width: min(96vw, 600px);
  border-radius: 20px;
  box-shadow: 0 20px 48px var(--shadow-color);
  overflow: hidden;
  background: var(--card);
  color: var(--text);
}

/* Header */
.epm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border);
  cursor: move;
}
.epm-title { margin: 0; font-size: 1.05rem; font-weight: 700; }
.epm-close { border: none; background: transparent; color: var(--text); font-size: 20px; cursor: pointer; }

/* Content */
.epm-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  padding: 14px 16px;
}

/* Left: image */
.epm-dropzone {
  position: relative;
  border: 1px dashed var(--border);
  border-radius: 14px;
  aspect-ratio: 16 / 9;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--input-bg);
}
.epm-dropzone.is-dragover { background: var(--input-bg-hover); }
.epm-dropzone img { width: 100%; height: 100%; object-fit: cover; border-radius: 12px; }
.epm-placeholder { color: var(--muted); font-size: .9rem; display:flex; align-items:center; justify-content:center; width:100%; height:100%; }
.epm-fallback-img { width: 60%; height: 60%; object-fit: contain; }
.epm-dropzone-actions { position: absolute; bottom: 8px; right: 8px; display: flex; gap: 6px; }

/* Right: form */
.form-row { display: flex; flex-direction: column; gap: 6px; margin-bottom: 10px; }
.two-cols { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }

.input, .select, .textarea {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--input-bg);
  color: var(--text);
  outline: none;
  transition: border-color .18s ease, background .18s ease;
}
.input:focus, .select:focus, .textarea:focus { border-color: var(--primary); background: var(--input-bg-hover); }

/* Multiselect */
.multiselect { display: flex; flex-wrap: wrap; gap: 6px; }
.chip {
  padding: 6px 10px; border-radius: 999px; border: 1px solid var(--border);
  background: var(--input-bg); color: var(--text); cursor: pointer; transition: all .18s ease;
}
.chip:hover { background: var(--input-bg-hover); }
.chip.selected { background: color-mix(in srgb, var(--primary) 25%, transparent); border-color: var(--primary); }

/* Footer */
.epm-footer {
  display: flex; gap: 10px; justify-content: flex-end; padding: 12px 16px; border-top: 1px solid var(--border);
}

.btn { border: 1px solid transparent; border-radius: 10px; padding: 10px 14px; cursor: pointer; transition: all .18s ease; }
.btn-primary { background: var(--primary); color: #fff; }
.btn-primary:hover { background: var(--primary-600); }
.btn-secondary { background: var(--input-bg); color: var(--text); border-color: var(--border); }
.btn-secondary:hover { background: var(--input-bg-hover); }

.loader { width: 14px; height: 14px; border: 2px solid var(--border); border-top-color: var(--text); border-radius: 50%; display: inline-block; margin-right: 8px; animation: spin 700ms linear infinite; }

/* Animations */
@keyframes fade-in { from { opacity: 0 } to { opacity: 1 } }
@keyframes spin { to { transform: rotate(360deg) } }

@media (max-width: 640px) { .epm-content { grid-template-columns: 1fr; } }

/* Toggle switch */
.switch { position: relative; display: inline-block; width: 42px; height: 24px; }
.switch input { opacity: 0; width: 0; height: 0; }
.slider { position: absolute; cursor: pointer; inset: 0; background: var(--input-bg); border: 1px solid var(--border); border-radius: 999px; transition: background .18s ease; }
.slider:before { content: ''; position: absolute; height: 18px; width: 18px; left: 3px; top: 50%; transform: translateY(-50%); background: #fff; border-radius: 50%; transition: transform .18s ease; }
.switch input:checked + .slider { background: var(--primary); border-color: var(--primary); }
.switch input:checked + .slider:before { transform: translate(18px, -50%); }
</style>
