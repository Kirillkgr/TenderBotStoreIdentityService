<template>
  <div class="tag-list">
    <div v-if="loading" class="loading">Загрузка тегов...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <template v-else>
      <div v-if="tags.length === 0" class="no-tags">
        Теги не найдены
      </div>
      <ul v-else class="tag-items">
        <li v-for="tag in tags" :key="tag.id" class="tag-item">
          <div class="tag-content">
            <span class="tag-title">
              {{ tag.name }}
              <button
                class="btn btn-link btn-sm ms-2"
                @click="$emit('edit', tag)"
                title="Изменить тег"
              >
                Изменить
              </button>
            </span>
            <div class="tag-actions">
              <button 
                v-if="canAddChildren" 
                @click="$emit('add-child', tag)"
                class="btn btn-sm btn-outline-primary"
              >
                + Добавить дочерний
              </button>
              <button 
                @click="$emit('edit', tag)"
                class="btn btn-sm btn-outline-secondary"
              >
                Редактировать
              </button>
              <button 
                @click="$emit('delete', tag)"
                class="btn btn-sm btn-outline-danger"
                :disabled="tag.childrenCount > 0"
                :title="tag.childrenCount > 0 ? 'Сначала удалите дочерние теги' : ''"
              >
                Удалить
              </button>
            </div>
          </div>
          <tag-list
            v-if="expandedTags.includes(tag.id)"
            :brand-id="brandId"
            :parent-id="tag.id"
            @tag-selected="$emit('tag-selected', $event)"
            @tag-updated="handleTagUpdated"
            @tag-deleted="handleTagDeleted"
          />
        </li>
      </ul>
    </template>
  </div>
</template>

<script>
import { ref, onMounted, watch } from 'vue';
import { useTagStore } from '@/store/tag';

export default {
  name: 'TagList',
  props: {
    brandId: {
      type: [Number, String],
      required: true
    },
    parentId: {
      type: [Number, String],
      default: 0
    },
    canAddChildren: {
      type: Boolean,
      default: true
    },
    autoLoad: {
      type: Boolean,
      default: true
    }
  },
  emits: ['tag-selected', 'tag-updated', 'tag-deleted', 'add-child', 'edit', 'delete'],
  setup(props, { emit }) {
    const tagStore = useTagStore();
    const loading = ref(false);
    const error = ref(null);
    const tags = ref([]);
    const expandedTags = ref([]);

    const loadTags = async () => {
      if (!props.brandId) return;
      
      loading.value = true;
      error.value = null;
      
      try {
        tags.value = await tagStore.fetchTagsByBrand(props.brandId, props.parentId);
        // Автоматически разворачиваем родительский тег, если есть дочерние
        if (props.parentId > 0 && tags.value.length > 0) {
          expandedTags.value.push(parseInt(props.parentId));
        }
      } catch (err) {
        error.value = err.message || 'Ошибка при загрузке тегов';
        console.error('Ошибка при загрузке тегов:', err);
      } finally {
        loading.value = false;
      }
    };

    const handleTagUpdated = (updatedTag) => {
      emit('tag-updated', updatedTag);
      // Перезагружаем теги, если обновленный тег находится в текущем списке
      if (updatedTag.parentId === props.parentId) {
        loadTags();
      }
    };

    const handleTagDeleted = (deletedTag) => {
      emit('tag-deleted', deletedTag);
      // Удаляем тег из текущего списка
      tags.value = tags.value.filter(tag => tag.id !== deletedTag.id);
    };

    // Загружаем теги при монтировании компонента
    onMounted(() => {
      if (props.autoLoad) {
        loadTags();
      }
    });

    // Следим за изменением brandId или parentId
    watch([() => props.brandId, () => props.parentId], () => {
      if (props.autoLoad) {
        loadTags();
      }
    });

    return {
      loading,
      error,
      tags,
      expandedTags,
      handleTagUpdated,
      handleTagDeleted
    };
  }
};
</script>

<style scoped>
.tag-list {
  margin-left: 20px;
  padding-left: 10px;
  border-left: 1px solid var(--border);
}

.tag-item {
  margin: 8px 0;
  list-style: none;
}

.tag-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  background-color: var(--input-bg);
  border-radius: 8px;
  border: 1px solid var(--border);
  color: var(--text);
}

.tag-title {
  display: inline-flex;
  align-items: center;
}

.tag-actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 4px 8px;
  border: 1px solid var(--border);
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  background: var(--input-bg);
  color: var(--text);
}

.btn-link {
  background: transparent;
  border: none;
  color: var(--primary);
  text-decoration: underline;
}

.btn-sm {
  padding: 2px 6px;
  font-size: 12px;
}

.btn-outline-primary { color: var(--primary); border-color: var(--primary); background-color: transparent; }

.btn-outline-secondary { color: var(--muted); border-color: var(--border); background-color: transparent; }

.btn-outline-danger { color: var(--danger); border-color: var(--danger); background-color: transparent; }

.loading, .error, .no-tags {
  padding: 10px;
  text-align: center;
  color: var(--muted);
}

.error { color: var(--danger); }
</style>
