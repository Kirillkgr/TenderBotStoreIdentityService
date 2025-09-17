import { defineStore } from 'pinia';
import { ref } from 'vue';
import tagService from '@/services/tagService';

export const useTagStore = defineStore('tag', () => {
  // Состояние
  const tags = ref([]);
  const currentTagTree = ref([]);
  const isLoading = ref(false);
  const error = ref(null);

  // Действия
  // Загрузка тегов по бренду и родительскому ID
  const fetchTagsByBrand = async (brandId, parentId = null) => {
    isLoading.value = true;
    error.value = null;
    try {
      // Загружаем только корневые теги, если parentId не указан
      const response = await tagService.getTagsByBrand(brandId, parentId);
      
      // Если загружаем корневые теги, обновляем список
      if (parentId === null || parentId === 0) {
        tags.value = response;
      }
      
      return response;
    } catch (err) {
      const errorMessage = err.response?.data?.message || 'Ошибка при загрузке тегов';
      error.value = errorMessage;
      console.error('Ошибка при загрузке тегов:', errorMessage, err);
      throw new Error(errorMessage);
    } finally {
      isLoading.value = false;
    }
  };

  // Загрузка полного дерева тегов (если эндпоинт будет реализован)
  const fetchTagTree = async (brandId) => {
    isLoading.value = true;
    error.value = null;
    try {
      // Пока используем загрузку корневых тегов, так как эндпоинт /tree/ не работает
      const response = await tagService.getTagsByBrand(brandId, 0);
      currentTagTree.value = response;
      return response;
    } catch (err) {
      // В случае ошибки возвращаем пустой массив, чтобы не ломать UI
      console.warn('Не удалось загрузить дерево тегов, загружаем только корневые теги', err);
      return [];
    } finally {
      isLoading.value = false;
    }
  };

  const createTag = async (tagData) => {
    isLoading.value = true;
    error.value = null;
    try {
      const response = await tagService.createTag(tagData);
      // Обновляем список тегов после создания
      if (tagData.brandId) {
        await fetchTagsByBrand(tagData.brandId, tagData.parentId || 0);
      }
      return response;
    } catch (err) {
      error.value = err.response?.data?.message || 'Ошибка при создании тега';
      console.error('Ошибка при создании тега:', err);
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  const updateTag = async ({ id, ...tagData }) => {
    isLoading.value = true;
    error.value = null;
    try {
      const response = await tagService.updateTag(id, tagData);
      // Обновляем список тегов после обновления
      if (tagData.brandId) {
        await fetchTagsByBrand(tagData.brandId, tagData.parentId || 0);
      }
      return response;
    } catch (err) {
      error.value = err.response?.data?.message || 'Ошибка при обновлении тега';
      console.error('Ошибка при обновлении тега:', err);
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  const deleteTag = async (tagId) => {
    isLoading.value = true;
    error.value = null;
    try {
      const response = await tagService.deleteTag(tagId);
      // Удаляем тег из текущего списка
      tags.value = tags.value.filter(tag => tag.id !== tagId);
      return response;
    } catch (err) {
      error.value = err.response?.data?.message || 'Ошибка при удалении тега';
      console.error('Ошибка при удалении тега:', err);
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  // Сброс состояния
  const reset = () => {
    tags.value = [];
    currentTagTree.value = [];
    error.value = null;
  };

  return {
    // Состояние
    tags,
    currentTagTree,
    isLoading,
    error,
    
    // Действия
    fetchTagsByBrand,
    fetchTagTree,
    createTag,
    updateTag,
    deleteTag,
    reset
  };
});
