<template>
  <Modal :is-modal-visible="true" @close="$emit('close')">
    <template #header>
      <h3>Редактировать тег</h3>
    </template>
    <template #content>
      <!-- Контекст -->
      <div class="context-info">
        <div class="path" v-if="currentPath">
          Место создания: <span class="path-text">{{ currentPath }}</span>
        </div>
      </div>

      <Form @submit="onSubmit" :validation-schema="schema" class="form-body">
        <div class="form-group">
          <label for="name">Название тега</label>
          <Field
            name="name"
            type="text"
            id="name"
            class="form-control"
            placeholder="Введите название тега"
            v-model="formData.name"
            :disabled="isSubmitting"
          />
          <ErrorMessage name="name" class="error-message" />
        </div>

        <div class="form-group">
          <label for="brandId">Бренд</label>
          <Field
            as="select"
            name="brandId"
            id="brandId"
            class="form-control"
            v-model="formData.brandId"
            :disabled="isSubmitting"
          >
            <option value="">Выберите бренд</option>
            <option v-for="brand in brands" :key="brand.id" :value="brand.id">
              {{ brand.name }}
            </option>
          </Field>
          <ErrorMessage name="brandId" class="error-message" />
        </div>

        <div class="form-group" v-if="formData.brandId">
          <label for="parentId">Родительский тег</label>
          <Field
            as="select"
            name="parentId"
            id="parentId"
            class="form-control"
            v-model="formData.parentId"
            :disabled="isSubmitting || loadingParents"
          >
            <option :value="'0'">Без родительского тега (корневой уровень)</option>
            <option v-for="tag in availableTags" :key="tag.id" :value="String(tag.id)">
              {{ tag.name }}{{ (props.tag?.parentId === tag.id) ? ' (текущий)' : '' }}
            </option>
          </Field>
          <div v-if="loadingParents" class="loading-hint">
            <span class="spinner-border spinner-border-sm me-1"></span>
            Загрузка тегов...
          </div>
          <ErrorMessage name="parentId" class="error-message" />
          <div class="text-muted mt-1" style="font-size: 12px;">
            Текущий родитель: {{ selectedParentName || 'Корень' }}
          </div>
        </div>

        <div class="form-actions">
          <button type="button" class="btn btn-outline-secondary" @click="$emit('close')" :disabled="isSubmitting">
            Отмена
          </button>
          <button type="submit" class="btn btn-primary" :disabled="isSubmitting">
            <span v-if="isSubmitting" class="spinner-border spinner-border-sm me-1" role="status"></span>
            Сохранить изменения
          </button>
        </div>
      </Form>
    </template>
  </Modal>
</template>

<script setup>
import { ErrorMessage, Field, Form } from 'vee-validate';
import * as yup from 'yup';
import { ref, computed, onMounted, watch } from 'vue';
import { useToast } from 'vue-toastification';
import Modal from '@/components/Modal.vue';
import tagService from '@/services/tagService';
import { useTagStore } from '@/store/tag';

const props = defineProps({
  brands: { type: Array, required: true, default: () => [] },
  tag: { type: Object, required: true },
  brandId: { type: [Number, String], default: null }
});

const emit = defineEmits(['close', 'saved']);
const toast = useToast();

const isSubmitting = ref(false);
const loadingParents = ref(false);
const availableTags = ref([]);

// ВАЖНО: инициализируем store ДО watcher'ов, чтобы не было ReferenceError в ранних вызовах
const tagStore = useTagStore();

const selectedBrandName = computed(() => props.brands.find(b => b.id === Number(formData.value.brandId))?.name || '');
const selectedParentName = computed(() => availableTags.value.find(t => t.id === Number(formData.value.parentId))?.name?.replace(/^—\s+/,'') || (Number(formData.value.parentId) ? '' : 'Корень'));
const currentPath = computed(() => {
  const parts = [];
  if (selectedBrandName.value) parts.push(selectedBrandName.value);
  if (formData.value.parentId && selectedParentName.value) parts.push(selectedParentName.value);
  return parts.length ? `/${parts.join('/')}/` : '';
});

const formData = ref({
  name: props.tag?.name || '',
  brandId: props.tag?.brandId || props.brandId || '',
  parentId: String(props.tag?.parentId ?? 0)
});

const schema = computed(() => yup.object({
  name: yup.string().required('Введите название тега').max(100, 'Название не должно превышать 100 символов'),
  brandId: yup.number().required('Выберите бренд').positive('Выберите бренд'),
  parentId: yup.number()
    .transform((_, original) => {
      if (original === '' || original == null) return 0;
      const n = parseInt(original, 10);
      return Number.isNaN(n) ? 0 : n;
    })
    .nullable()
    .test('not-self-parent', 'Нельзя выбрать текущий тег как родительский', v => v !== (props.tag?.id ?? -1))
}));

watch(() => formData.value.brandId, async (newBrandId) => {
  if (newBrandId) {
    await loadAvailableTags(Number(newBrandId), props.tag?.id);
    formData.value.parentId = String(props.tag?.parentId ?? 0);
  } else {
    availableTags.value = [];
  }
}, { immediate: true });

watch(availableTags, () => {
  formData.value.parentId = String(props.tag?.parentId ?? 0);
});

onMounted(async () => {
  // Начальная загрузка выполняется через watch(..., { immediate: true })
});

async function loadAvailableTags(brandId, excludeId = null) {
  loadingParents.value = true;
  availableTags.value = [];
  try {
    // 1) Быстрый путь: один запрос дерева
    const tree = await tagService.getTagTree(brandId);
    let roots;
    if (Array.isArray(tree) && tree.length > 0) {
      const annotate = (nodes, parentId = 0) => {
        return nodes.map(n => ({
          id: n.id,
          name: n.name,
          parentId: n.parentId ?? parentId,
          level: n.level ?? 0,
          children: Array.isArray(n.children) ? annotate(n.children, n.id) : []
        }));
      };
      roots = annotate(tree, 0);
    } else {
      // 2) Fallback: BFS через admin API by-brand
      const queue = [0];
      const visited = new Set();
      const collected = [];
      while (queue.length) {
        const pid = queue.shift();
        if (visited.has(pid)) continue;
        visited.add(pid);
        const level = await tagStore.fetchTagsByBrand(brandId, pid);
        (level || []).forEach(t => {
          collected.push(t);
          queue.push(t.id);
        });
      }
      const byId2 = new Map();
      collected.forEach(t => byId2.set(t.id, { ...t, children: [] }));
      byId2.forEach(t => {
        const pid = t.parentId ?? 0;
        if (pid && byId2.has(pid)) byId2.get(pid).children.push(t);
      });
      roots = [...byId2.values()].filter(t => (t.parentId ?? 0) === 0);
    }

    const flatten = (items, level = 0) => {
      let res = [];
      items.forEach(item => {
        if (excludeId && item.id === excludeId) {
          // skip self
        } else {
          const indent = '— '.repeat(level);
          res.push({ ...item, name: `${indent}${item.name}`, level });
        }
        if (item.children?.length) res = res.concat(flatten(item.children, level + 1));
      });
      return res;
    }

    availableTags.value = flatten(roots);
  } catch (e) {
    console.error('Ошибка при загрузке тегов:', e);
    toast.error(e?.message || 'Не удалось загрузить список тегов');
  } finally {
    loadingParents.value = false;
  }
};

const onSubmit = async (values) => {
  try {
    isSubmitting.value = true;
    const payload = {
      name: values.name.trim(),
      brandId: Number(values.brandId),
      parentId: values.parentId ? Number(values.parentId) : 0,
    };
    const result = await tagService.updateFull(props.tag.id, payload);
    toast.success('Тег успешно обновлен!');
    emit('saved', result);
    emit('close');
  } catch (e) {
    console.error('Ошибка при сохранении тега:', e);
    toast.error(e?.message || 'Произошла ошибка при сохранении тега');
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<style scoped>
.context-info { margin-bottom: 12px; text-align: left; }
.path { font-size: 13px; color: #94a3b8; }
.path-text { color: #e2e8f0; font-weight: 600; }
.form-body { display: flex; flex-direction: column; gap: 20px; width: 100%; max-width: 480px; margin: 0 auto; padding: 0 10px; }
.form-group { text-align: left; margin-bottom: 1rem; }
.form-group label { display: block; margin-bottom: 8px; font-weight: 600; color: #e2e8f0; font-size: 14px; }
.form-control, .form-select { width: 100%; padding: 12px 16px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 15px; background-color: #ffffff; transition: all 0.2s ease; color: #111827; }
.form-control::placeholder { color: #6b7280; opacity: 1; }
.form-control:focus, .form-select:focus { outline: none; border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.2); }
.form-control:disabled, .form-select:disabled { background-color: #f3f4f6; color: #9ca3af; }
.error-message { color: #dc3545; font-size: 12px; margin-top: 4px; }
.form-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.btn { display: inline-flex; align-items: center; justify-content: center; padding: 0.5rem 0.75rem; border-radius: 0.25rem; cursor: pointer; border: 1px solid transparent; }
.btn.btn-primary { color: #fff; background-color: #0d6efd; border-color: #0d6efd; }
.btn.btn-outline-secondary { color: #fff; background-color: transparent; border-color: #6c757d; }
.spinner-border { width: 1rem; height: 1rem; border-width: 0.2em; }
</style>
