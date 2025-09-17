<template>
  <div class="tag-form">
    <form @submit.prevent="handleSubmit">
      <div class="mb-3">
        <label for="tagName" class="form-label">Название тега</label>
        <input
          type="text"
          class="form-control"
          id="tagName"
          v-model="formData.name"
          required
          :disabled="loading"
        />
        <div v-if="errors.name" class="invalid-feedback d-block">
          {{ errors.name }}
        </div>
      </div>

      <div class="mb-3" v-if="showParentSelect">
        <label for="parentId" class="form-label">Родительский тег</label>
        <select
          class="form-select"
          id="parentId"
          v-model="formData.parentId"
          :disabled="loading || !availableParents.length"
        >
          <option :value="0">Без родительского тега</option>
          <option 
            v-for="parent in availableParents" 
            :key="parent.id" 
            :value="parent.id"
          >
            {{ parent.name }}
          </option>
        </select>
        <div v-if="errors.parentId" class="invalid-feedback d-block">
          {{ errors.parentId }}
        </div>
      </div>

      <div class="d-flex justify-content-between">
        <button 
          type="button" 
          class="btn btn-secondary" 
          @click="$emit('cancel')"
          :disabled="loading"
        >
          Отмена
        </button>
        <button 
          type="submit" 
          class="btn btn-primary"
          :disabled="loading"
        >
          <span v-if="loading" class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
          {{ submitButtonText }}
        </button>
      </div>
    </form>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue';
import { useTagStore } from '@/store/tag';

export default {
  name: 'TagForm',
  props: {
    brandId: {
      type: [Number, String],
      required: true
    },
    tag: {
      type: Object,
      default: null
    },
    parentId: {
      type: [Number, String],
      default: 0
    },
    showParentSelect: {
      type: Boolean,
      default: true
    },
    availableParents: {
      type: Array,
      default: () => []
    }
  },
  emits: ['submit', 'cancel'],
  setup(props, { emit }) {
    // const tagStore = useTagStore();
    const loading = ref(false);
    const errors = ref({});
    
    const formData = ref({
      name: '',
      brandId: props.brandId,
      parentId: props.parentId
    });

    const submitButtonText = computed(() => {
      return props.tag ? 'Сохранить изменения' : 'Создать тег';
    });

    // Инициализация формы, если передан существующий тег
    const initForm = () => {
      if (props.tag) {
        formData.value = {
          ...formData.value,
          ...props.tag,
          // Убедимся, что brandId и parentId - числа
          brandId: parseInt(props.tag.brandId || props.brandId),
          parentId: parseInt(props.tag.parentId || props.parentId)
        };
      } else {
        formData.value = {
          name: '',
          brandId: parseInt(props.brandId),
          parentId: parseInt(props.parentId) || 0
        };
      }
    };

    // Обработка отправки формы
    const handleSubmit = async () => {
      if (!validateForm()) return;
      
      loading.value = true;
      errors.value = {};
      
      try {
        // Преобразуем значения в числа, если это необходимо
        const dataToSubmit = {
          ...formData.value,
          brandId: parseInt(formData.value.brandId),
          parentId: formData.value.parentId ? parseInt(formData.value.parentId) : 0
        };
        
        emit('submit', dataToSubmit);
      } catch (error) {
        console.error('Ошибка при обработке формы:', error);
        if (error.response?.data?.errors) {
          errors.value = error.response.data.errors;
        } else {
          errors.value.general = error.message || 'Произошла ошибка при сохранении тега';
        }
      } finally {
        loading.value = false;
      }
    };

    // Валидация формы
    const validateForm = () => {
      const newErrors = {};
      
      if (!formData.value.name || formData.value.name.trim() === '') {
        newErrors.name = 'Введите название тега';
      }
      
      if (formData.value.parentId && isNaN(parseInt(formData.value.parentId))) {
        newErrors.parentId = 'Неверный формат родительского тега';
      }
      
      errors.value = newErrors;
      return Object.keys(newErrors).length === 0;
    };

    // Инициализация формы при монтировании
    onMounted(() => {
      initForm();
    });

    // Обновляем форму при изменении входных данных
    watch([() => props.tag, () => props.brandId, () => props.parentId], () => {
      initForm();
    });

    return {
      formData,
      loading,
      errors,
      submitButtonText,
      handleSubmit
    };
  }
};
</script>

<style scoped>
.tag-form {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.form-label {
  font-weight: 500;
  margin-bottom: 0.5rem;
}

.form-control,
.form-select {
  padding: 0.5rem 0.75rem;
  font-size: 1rem;
  line-height: 1.5;
  border: 1px solid #ced4da;
  border-radius: 0.25rem;
  transition: border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out;
}

.form-control:focus,
.form-select:focus {
  border-color: #86b7fe;
  outline: 0;
  box-shadow: 0 0 0 0.25rem rgba(13, 110, 253, 0.25);
}

.invalid-feedback {
  width: 100%;
  margin-top: 0.25rem;
  font-size: 0.875em;
  color: #dc3545;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.375rem 0.75rem;
  font-size: 1rem;
  font-weight: 400;
  line-height: 1.5;
  text-align: center;
  text-decoration: none;
  white-space: nowrap;
  vertical-align: middle;
  cursor: pointer;
  user-select: none;
  border: 1px solid transparent;
  border-radius: 0.25rem;
  transition: color 0.15s ease-in-out, background-color 0.15s ease-in-out,
    border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out;
}

.btn-primary {
  color: #fff;
  background-color: #0d6efd;
  border-color: #0d6efd;
}

.btn-primary:hover {
  background-color: #0b5ed7;
  border-color: #0a58ca;
}

.btn-primary:disabled {
  background-color: #86b7fe;
  border-color: #86b7fe;
  opacity: 0.65;
}

.btn-secondary {
  color: #fff;
  background-color: #6c757d;
  border-color: #6c757d;
}

.btn-secondary:hover {
  background-color: #5c636a;
  border-color: #565e64;
}

.btn-secondary:disabled {
  background-color: #6c757d;
  border-color: #6c757d;
  opacity: 0.65;
}

.spinner-border {
  width: 1rem;
  height: 1rem;
  border-width: 0.2em;
}
</style>
