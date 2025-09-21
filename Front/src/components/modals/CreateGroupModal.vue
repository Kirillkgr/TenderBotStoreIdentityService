<template>
  <Modal :is-modal-visible="true" @close="$emit('close')">
    <template #header>
      <h3>{{ editing ? 'Редактировать тег' : 'Создать новый тег' }}</h3>
    </template>
    <template #content>
      <!-- Текущий контекст -->
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
          <ErrorMessage name="name" class="error-message"/>
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
          <ErrorMessage name="brandId" class="error-message"/>
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
              {{ tag.name }}{{ editing && (props.tag?.parentId === tag.id) ? ' (текущий)' : '' }}
            </option>
          </Field>
          <div v-if="loadingParents" class="loading-hint">
            <span class="spinner-border spinner-border-sm me-1"></span>
            Загрузка тегов...
          </div>
          <ErrorMessage name="parentId" class="error-message"/>
          <div v-if="editing" class="text-muted mt-1" style="font-size: 12px;">
            Текущий родитель: {{ selectedParentName || 'Корень' }}
          </div>
        </div>

        <div class="form-actions">
          <button 
            type="button" 
            class="btn btn-outline-secondary" 
            @click="$emit('close')" 
            :disabled="isSubmitting"
          >
            Отмена
          </button>
          <button 
            type="submit" 
            class="btn btn-primary" 
            :disabled="isSubmitting"
          >
            <span v-if="isSubmitting" class="spinner-border spinner-border-sm me-1" role="status"></span>
            {{ editing ? 'Сохранить изменения' : 'Создать тег' }}
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
import { useTagStore } from '@/store/tag';
import tagService from '@/services/tagService';

const props = defineProps({
  brands: {
    type: Array,
    required: true,
    default: () => []
  },
  tag: {
    type: Object,
    default: null
  },
  brandId: {
    type: [Number, String],
    default: null
  },
  parentId: {
    type: [Number, String],
    default: null
  }
});

const emit = defineEmits(['close', 'saved']);
const toast = useToast();
const tagStore = useTagStore();

// Состояние UI
const isSubmitting = ref(false);
const loadingParents = ref(false);
const availableTags = ref([]);

// Вычисляемые свойства
const editing = computed(() => !!props.tag);
const selectedBrandName = computed(() => props.brands.find(b => b.id === Number(formData.value.brandId))?.name || '');
const selectedParentName = computed(() => availableTags.value.find(t => t.id === Number(formData.value.parentId))?.name?.replace(/^—\s+/,'') || (formData.value.parentId ? '' : 'Корень'));
const currentPath = computed(() => {
  const parts = [];
  if (selectedBrandName.value) parts.push(selectedBrandName.value);
  if (formData.value.parentId && selectedParentName.value) parts.push(selectedParentName.value);
  return parts.length ? `/${parts.join('/')}/` : '';
});

// Данные формы
const formData = ref({
  name: '',
  brandId: props.brandId || '',
  parentId: String(props.parentId ?? 0)
});

// Схема валидации (parentId приводим к числу; пустое => 0)
const schema = computed(() => yup.object({
  name: yup.string()
    .required('Введите название тега')
    .max(100, 'Название не должно превышать 100 символов'),
  brandId: yup.number()
    .required('Выберите бренд')
    .positive('Выберите бренд'),
  parentId: yup.number()
    .transform((value, originalValue) => {
      if (originalValue === '' || originalValue === null || originalValue === undefined) return 0;
      const n = parseInt(originalValue, 10);
      return isNaN(n) ? 0 : n;
    })
    .nullable()
    .test(
      'not-self-parent',
      'Нельзя выбрать текущий тег как родительский',
      value => !editing.value || value !== (props.tag?.id ?? -1)
    )
}));

// Загрузка доступных родительских тегов при изменении бренда
watch(() => formData.value.brandId, async (newBrandId) => {
  if (newBrandId) {
    await loadAvailableTags(Number(newBrandId), props.tag?.id);
    // После загрузки списка родителей убеждаемся, что выбран текущий родитель при редактировании
    if (editing.value) {
      formData.value.parentId = props.tag?.parentId ? String(props.tag.parentId) : '0';
    }
  } else {
    availableTags.value = [];
  }
}, { immediate: true });

// Если список доступных родителей обновился позже, продублируем установку выбранного родителя
watch(availableTags, () => {
  if (editing.value) {
    formData.value.parentId = props.tag?.parentId ? String(props.tag.parentId) : '0';
  }
});

// Инициализация формы
onMounted(async () => {
  if (props.tag) {
    // Редактирование существующего тега
    formData.value = {
      name: props.tag.name,
      brandId: props.tag.brandId || props.brandId || '',
      parentId: String(props.tag.parentId || 0)
    };
    
    if (props.tag.brandId) {
      await loadAvailableTags(props.tag.brandId, props.tag.id);
      // Гарантируем автоподстановку текущего родителя
      formData.value.parentId = String(props.tag.parentId || 0);
    }
  } else if (props.brandId) {
    // Создание нового тега с предвыбранным брендом
    formData.value.brandId = props.brandId;
    // Предварительно устанавливаем родителя из текущего контекста, если он передан
    if (props.parentId !== null && props.parentId !== undefined) {
      formData.value.parentId = String(props.parentId);
    }
    await loadAvailableTags(Number(props.brandId));
  }
  
  // Если передан parentId, устанавливаем его как родительский
  if (props.parentId !== null && props.parentId !== undefined) {
    formData.value.parentId = String(props.parentId);
  }
});

/**
 * Загрузка доступных родительских тегов
 * @param {number} brandId - ID бренда
 * @param {number|null} excludeId - ID тега, который нужно исключить (например, текущий редактируемый тег)
 */
async function loadAvailableTags(brandId, excludeId = null) {
  if (!brandId) {
    availableTags.value = [];
    return;
  }

  loadingParents.value = true;
  availableTags.value = [];

  try {
    // 1) Быстрый путь: один запрос дерева
    const tree = await tagService.getTagTree(brandId);
    let roots;
    if (Array.isArray(tree) && tree.length > 0) {
      // Ответ уже в виде дерева DTO (children)
      // Нормализуем: parentId может отсутствовать у узлов дерева
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
      // 2) Fallback: BFS по уровням через admin API (by-brand/parentId)
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

    const flattenWithIndent = (items, level = 0) => {
      let result = [];
      items.forEach(item => {
        if (excludeId && item.id === excludeId) {
          // Пропускаем исключаемый id
        } else {
          const indent = '— '.repeat(level);
          result.push({ ...item, name: `${indent}${item.name}`, level });
        }
        if (item.children && item.children.length > 0) {
          result = result.concat(flattenWithIndent(item.children, level + 1));
        }
      });
      return result;
    };

    availableTags.value = flattenWithIndent(roots);
  } catch (error) {
    console.error('Ошибка при загрузке тегов:', error);
    const errorMessage = error.message || 'Не удалось загрузить список тегов';
    toast.error(errorMessage);
  } finally {
    loadingParents.value = false;
  }
}

/**
 * Обработка отправки формы
 */
const onSubmit = async (values, { resetForm }) => {
  try {
    isSubmitting.value = true;

    const newName = values.name.trim();
    const newParentId = values.parentId ? parseInt(values.parentId, 10) : 0;
    const newBrandId = parseInt(values.brandId, 10);

    console.log('Сохранение тега:', { newName, newParentId, newBrandId });

    let result;
    if (editing.value) {
      // Единый запрос: бренд/родитель/имя одним вызовом
      result = await tagService.updateFull(props.tag.id, {
        name: newName,
        parentId: newParentId,
        brandId: newBrandId
      });
      toast.success('Тег успешно обновлен!');
    } else {
      // Создание нового тега
      const tagData = { name: newName, brandId: newBrandId, parentId: newParentId };
      console.log('Создание нового тега', tagData);
      result = await tagService.createTag(tagData);
      toast.success('Тег успешно создан!');
    }

    // Обновляем список доступных тегов, если изменился бренд
    if (formData.brandId !== values.brandId) {
      await loadAvailableTags(values.brandId);
    }

    resetForm();
    emit('saved', result);
    emit('close');

  } catch (error) {
    console.error('Ошибка при сохранении тега:', error);
    const errorMessage = error.response?.data?.message || error.message || 'Произошла ошибка при сохранении тега';
    toast.error(errorMessage);
    
    // Прокручиваем к верху формы, чтобы пользователь увидел сообщение об ошибке
    const form = document.querySelector('.form-body');
    if (form) {
      form.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<style scoped>
.context-info {
  margin-bottom: 12px;
  text-align: left;
}
.path {
  font-size: 13px;
  color: #94a3b8;
}
.path-text {
  color: #e2e8f0;
  font-weight: 600;
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
  padding: 0 10px;
}

.form-group {
  text-align: left;
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #e2e8f0; /* Высокий контраст на темном фоне */
  font-size: 14px;
}

.form-control, .form-select {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 15px;
  background-color: #ffffff;
  transition: all 0.2s ease;
  color: #111827; /* Текст в инпуте более темный и читаемый */
}

.form-control::placeholder {
  color: #6b7280; /* Читаемый placeholder */
  opacity: 1;
}

.form-control:focus, .form-select:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.2);
}

.form-control:disabled, .form-select:disabled {
  background-color: #f3f4f6;
  color: #374151; /* Чтобы текст был читаем даже в disabled */
  cursor: not-allowed;
  opacity: 1;
}

.checkbox-input {
  display: none; /* Hide default checkbox */
}

.checkbox-label {
  position: relative;
  padding-left: 30px;
  cursor: pointer;
  user-select: none;
  color: var(--text-primary);
}

.checkbox-label::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  border: 1px solid #ccc;
  border-radius: 6px;
  background-color: #fff;
  transition: all 0.2s ease;
}

.checkbox-input:checked + .checkbox-label::before {
  background-color: var(--success-color, #28a745);
  border-color: var(--success-color, #28a745);
}

.checkbox-label::after {
  content: '✓';
  position: absolute;
  left: 4px;
  top: 50%;
  transform: translateY(-50%) scale(0);
  font-size: 16px;
  color: white;
  transition: transform 0.2s ease;
}

.checkbox-input:checked + .checkbox-label::after {
  transform: translateY(-50%) scale(1);
}

.error-message {
  color: #ff3b30;
  font-size: 13px;
  padding-top: 4px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
}

.btn {
  padding: 12px 24px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease-in-out;
  border: none;
  text-align: center;
}

.btn-primary {
  background-color: var(--primary-color);
  color: white;
  box-shadow: 0 4px 15px rgba(0, 122, 255, 0.2);
}

.btn-primary:hover {
  background-color: var(--primary-color-dark);
  box-shadow: 0 6px 20px rgba(0, 122, 255, 0.3);
  transform: translateY(-2px);
}

.btn-secondary {
  background-color: #f0f2f5;
  color: var(--text-primary);
}

.btn-secondary:hover {
  background-color: #e4e6e9;
  transform: translateY(-2px);
}

.btn-danger {
  background-color: #f0f2f5;
  color: #ff3b30;
}

.btn-danger:hover {
  background-color: #ff3b30;
  color: white;
  transform: translateY(-2px);
}
</style>
