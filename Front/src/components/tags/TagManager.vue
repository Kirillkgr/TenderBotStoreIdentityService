<template>
  <div class="tag-manager">
    <div class="tag-manager-header">
      <h2>Управление тегами</h2>
      <button 
        v-if="!showForm" 
        class="btn btn-primary"
        @click="openCreateForm"
      >
        + Создать тег
      </button>
    </div>

    <!-- Форма создания/редактирования тега -->
    <div v-if="showForm" class="tag-form-container">
      <h3>{{ formTitle }}</h3>
      <tag-form
        ref="tagForm"
        :brand-id="brandId"
        :tag="currentTag"
        :parent-id="editingParentId"
        :available-parents="availableParents"
        :show-parent-select="showParentSelect"
        @submit="handleSubmit"
        @cancel="closeForm"
      />
    </div>

    <!-- Дерево тегов -->
    <div class="tag-tree-container">
      <div v-if="loading && !tags.length" class="loading">
        <div class="spinner-border text-primary" role="status">
          <span class="visually-hidden">Загрузка...</span>
        </div>
        <p>Загрузка тегов...</p>
      </div>
      
      <div v-else-if="error" class="alert alert-danger">
        {{ error }}
      </div>
      
      <div v-else-if="!tags.length" class="no-tags">
        <p>Теги не найдены. Создайте первый тег.</p>
      </div>
      
      <tag-list
        v-else
        :brand-id="brandId"
        :parent-id="0"
        @tag-selected="handleTagSelected"
        @tag-updated="handleTagUpdated"
        @tag-deleted="handleTagDeleted"
        @add-child="handleAddChild"
        @edit="handleEditTag"
        @delete="confirmDeleteTag"
      />
    </div>

    <!-- Модальное окно подтверждения удаления -->
    <div v-if="showDeleteConfirm" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">Подтверждение удаления</h5>
          <button type="button" class="btn-close" @click="showDeleteConfirm = false"></button>
        </div>
        <div class="modal-body">
          <p>Вы уверены, что хотите удалить тег "{{ tagToDelete?.name }}"?</p>
          <p v-if="tagToDelete?.childrenCount > 0" class="text-warning">
            Внимание! Этот тег содержит дочерние теги. Все дочерние теги также будут удалены.
          </p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="showDeleteConfirm = false">
            Отмена
          </button>
          <button 
            type="button" 
            class="btn btn-danger" 
            @click="deleteTag"
            :disabled="deleting"
          >
            <span v-if="deleting" class="spinner-border spinner-border-sm me-1" role="status"></span>
            Удалить
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import { useTagStore } from '@/store/tag';
import { useToast } from 'vue-toastification';
import TagList from './TagList.vue';
import TagForm from './TagForm.vue';

export default {
  name: 'TagManager',
  components: {
    TagList,
    TagForm
  },
  props: {
    brandId: {
      type: [Number, String],
      required: true
    },
    // Можно ли создавать корневые теги (без родителя)
    allowRootTags: {
      type: Boolean,
      default: true
    },
    // Показывать ли выбор родительского тега при создании
    showParentSelect: {
      type: Boolean,
      default: true
    },
    // ID родительского тега по умолчанию (если не разрешены корневые теги)
    defaultParentId: {
      type: [Number, String],
      default: 0
    }
  },
  setup(props) {
    const tagStore = useTagStore();
    const toast = useToast();
    
    // Состояние
    const showForm = ref(false);
    const currentTag = ref(null);
    const editingParentId = ref(props.defaultParentId);
    const tagToDelete = ref(null);
    const showDeleteConfirm = ref(false);
    const deleting = ref(false);
    const loading = ref(false);
    const error = ref(null);
    const tags = ref([]);
    const availableParents = ref([]);
    
    // Вычисляемые свойства
    const formTitle = computed(() => {
      return currentTag.value ? 'Редактировать тег' : 'Создать тег';
    });
    
    // Загрузка тегов
    const loadTags = async () => {
      loading.value = true;
      error.value = null;
      
      try {
        // Загружаем корневые теги
        tags.value = await tagStore.fetchTagsByBrand(props.brandId, 0);
        
        // Загружаем все теги для выпадающего списка родителей
        if (props.showParentSelect) {
          const allTags = await tagStore.fetchTagTree(props.brandId);
          availableParents.value = flattenTagTree(allTags);
        }
      } catch (err) {
        console.error('Ошибка при загрузке тегов:', err);
        error.value = 'Не удалось загрузить теги. Пожалуйста, попробуйте позже.';
        toast.error('Ошибка при загрузке тегов');
      } finally {
        loading.value = false;
      }
    };
    
    // Преобразование дерева тегов в плоский список
    const flattenTagTree = (tagTree, level = 0) => {
      let result = [];
      
      for (const tag of tagTree) {
        // Добавляем отступы для вложенности
        const namePrefix = '— '.repeat(level) + (level > 0 ? ' ' : '');
        result.push({
          ...tag,
          name: namePrefix + tag.name,
          level
        });
        
        // Рекурсивно добавляем дочерние теги
        if (tag.children && tag.children.length) {
          result = result.concat(flattenTagTree(tag.children, level + 1));
        }
      }
      
      return result;
    };
    
    // Открытие формы создания тега
    const openCreateForm = (parentTag = null) => {
      currentTag.value = null;
      editingParentId.value = parentTag ? parentTag.id : props.defaultParentId;
      showForm.value = true;
    };
    
    // Открытие формы редактирования тега
    const handleEditTag = (tag) => {
      currentTag.value = { ...tag };
      showForm.value = true;
    };
    
    // Обработка добавления дочернего тега
    const handleAddChild = (parentTag) => {
      openCreateForm(parentTag);
    };
    
    // Закрытие формы
    const closeForm = () => {
      showForm.value = false;
      currentTag.value = null;
    };
    
    // Обработка сохранения тега
    const handleSubmit = async (tagData) => {
      try {
        if (currentTag.value) {
          // Обновление существующего тега
          await tagStore.updateTag({
            id: currentTag.value.id,
            ...tagData
          });
          toast.success('Тег успешно обновлен');
        } else {
          // Создание нового тега
          await tagStore.createTag(tagData);
          toast.success('Тег успешно создан');
        }
        
        // Перезагружаем теги и закрываем форму
        await loadTags();
        closeForm();
      } catch (error) {
        console.error('Ошибка при сохранении тега:', error);
        const errorMessage = error.response?.data?.message || 'Произошла ошибка при сохранении тега';
        toast.error(errorMessage);
      }
    };
    
    // Подтверждение удаления тега
    const confirmDeleteTag = (tag) => {
      tagToDelete.value = tag;
      showDeleteConfirm.value = true;
    };
    
    // Удаление тега
    const deleteTag = async () => {
      if (!tagToDelete.value) return;
      
      deleting.value = true;
      
      try {
        await tagStore.deleteTag(tagToDelete.value.id);
        toast.success('Тег успешно удален');
        await loadTags(); // Перезагружаем список тегов
      } catch (error) {
        console.error('Ошибка при удалении тега:', error);
        const errorMessage = error.response?.data?.message || 'Произошла ошибка при удалении тега';
        toast.error(errorMessage);
      } finally {
        deleting.value = false;
        showDeleteConfirm.value = false;
        tagToDelete.value = null;
      }
    };
    
    // Обработчик выбора тега
    const handleTagSelected = (tag) => {
      // Можно добавить логику обработки выбора тега
      console.log('Выбран тег:', tag);
    };
    
    // Обработчик обновления тега
    const handleTagUpdated = (tag) => {
      // Обновляем данные в локальном состоянии
      const updateTagInTree = (tags) => {
        return tags.map(t => {
          if (t.id === tag.id) {
            return { ...t, ...tag };
          }
          if (t.children && t.children.length) {
            return {
              ...t,
              children: updateTagInTree(t.children)
            };
          }
          return t;
        });
      };
      
      tags.value = updateTagInTree(tags.value);
    };
    
    // Обработчик удаления тега
    const handleTagDeleted = (deletedTag) => {
      // Удаляем тег из локального состояния
      const removeTagFromTree = (tags) => {
        return tags
          .filter(t => t.id !== deletedTag.id)
          .map(t => ({
            ...t,
            children: t.children ? removeTagFromTree(t.children) : []
          }));
      };
      
      tags.value = removeTagFromTree(tags.value);
    };
    
    // Загружаем теги при монтировании компонента
    onMounted(() => {
      loadTags();
    });
    
    // Следим за изменением brandId
    watch(() => props.brandId, () => {
      if (props.brandId) {
        loadTags();
      }
    }, { immediate: true });
    
    return {
      // Состояние
      showForm,
      currentTag,
      tagToDelete,
      showDeleteConfirm,
      deleting,
      loading,
      error,
      tags,
      availableParents,
      editingParentId,
      
      // Вычисляемые свойства
      formTitle,
      
      // Методы
      openCreateForm,
      handleEditTag,
      handleAddChild,
      closeForm,
      handleSubmit,
      confirmDeleteTag,
      deleteTag,
      handleTagSelected,
      handleTagUpdated,
      handleTagDeleted
    };
  }
};
</script>

<style scoped>
.tag-manager {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.tag-manager-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.tag-form-container {
  background-color: var(--card);
  color: var(--text);
  border: 1px solid var(--card-border);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 6px 16px var(--shadow-color);
}

.tag-tree-container {
  background-color: var(--card);
  color: var(--text);
  border: 1px solid var(--card-border);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 6px 16px var(--shadow-color);
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: var(--muted);
}

.no-tags {
  text-align: center;
  padding: 20px;
  color: var(--muted);
}

.alert {
  padding: 15px;
  margin-bottom: 20px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background-color: var(--card);
}

.alert-danger {
  color: #721c24;
  background-color: var(--card);
  border-color: var(--border);
  background-color: #f8d7da;
  border-color: #f5c6cb;
}

.text-warning {
  color: #856404;
}

/* Модальное окно */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
}

.modal-content {
  background-color: #fff;
  border-radius: 8px;
  width: 100%;
  max-width: 500px;
  margin: 0 15px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.5);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #e9ecef;
}

.modal-title {
  margin: 0;
  line-height: 1.5;
}

.btn-close {
  padding: 0.5rem;
  margin: -0.5rem -0.5rem -0.5rem auto;
  background-color: transparent;
  border: 0;
  border-radius: 0.25rem;
  opacity: 0.5;
  cursor: pointer;
  font-size: 1.5rem;
  line-height: 1;
  color: #000;
}

.modal-body {
  padding: 20px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 15px 20px;
  border-top: 1px solid #e9ecef;
  gap: 10px;
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

.btn-secondary {
  color: #fff;
  background-color: #6c757d;
  border-color: #6c757d;
}

.btn-secondary:hover {
  background-color: #5c636a;
  border-color: #565e64;
}

.btn-danger {
  color: #fff;
  background-color: #dc3545;
  border-color: #dc3545;
}

.btn-danger:hover {
  background-color: #bb2d3b;
  border-color: #b02a37;
}

.btn-danger:disabled {
  background-color: #dc3545;
  border-color: #dc3545;
  opacity: 0.65;
}

.spinner-border {
  width: 1rem;
  height: 1rem;
  border-width: 0.2em;
  margin-right: 0.5rem;
}
</style>
