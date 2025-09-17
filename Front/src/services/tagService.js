import api from './api';
import { useAuthStore } from '@/store/auth';

const API_PREFIX = '/auth/v1/group-tags';
const PUBLIC_PREFIX = '/menu/v1/group-tags';

// Функция для обработки ошибок API
const handleApiError = (error, defaultMessage = 'Произошла ошибка') => {
  console.error('API Error:', error);
  
  if (error.response) {
    // Ошибка от сервера с кодом состояния
    const { status, data } = error.response;
    
    if (status === 401) {
      // Пробуем обновить токен, если истек срок действия
      const authStore = useAuthStore();
      if (authStore.isAuthenticated) {
        return authStore.refreshToken()
          .then(() => {
            // Повторяем запрос после обновления токена
            return error.config.retryCount < 3 
              ? api({
                  ...error.config,
                  retryCount: (error.config.retryCount || 0) + 1
                })
              : Promise.reject(new Error('Не удалось обновить сессию. Пожалуйста, войдите снова.'));
          })
          .catch(refreshError => {
            console.error('Ошибка при обновлении токена:', refreshError);
            throw new Error('Сессия истекла. Пожалуйста, войдите снова.');
          });
      }
    }
    
    // Возвращаем сообщение об ошибке от сервера или стандартное сообщение
    const errorMessage = data?.message || data?.error || defaultMessage;
    return Promise.reject(new Error(errorMessage));
  } else if (error.request) {
    // Запрос был отправлен, но ответ не получен
    return Promise.reject(new Error('Сервер не отвечает. Пожалуйста, проверьте подключение к интернету.'));
  }
  
  // Ошибка при настройке запроса
  return Promise.reject(new Error('Ошибка при выполнении запроса. Пожалуйста, попробуйте позже.'));
};

export const tagService = {
  // Создание нового тега
  async createTag(tagData) {
    try {
      console.log('Создание тега:', tagData);
      const response = await api.post(API_PREFIX, tagData);
      console.log('Тег успешно создан:', response.data);
      return response.data;
    } catch (error) {
      return handleApiError(error, 'Не удалось создать тег');
    }
  },

  // ПУБЛИЧНЫЕ ЗАПРОСЫ ДЛЯ ГЛАВНОЙ (menu/v1)
  async getPublicTagsByBrand(brandId, parentId = null) {
    try {
      const params = {};
      if (parentId !== null && parentId !== undefined) {
        params.parentId = parentId;
      }
      const response = await api.get(`${PUBLIC_PREFIX}/by-brand/${brandId}`, { params });
      return Array.isArray(response.data) ? response.data : (response.data?.data ?? []);
    } catch (error) {
      return handleApiError(error, 'Не удалось загрузить публичные теги');
    }
  },

  async getPublicTagTree(brandId) {
    try {
      const response = await api.get(`${PUBLIC_PREFIX}/tree/${brandId}`);
      return response.data;
    } catch (error) {
      // Возвращаем пустой массив, чтобы не ломать UI
      return [];
    }
  },

  // Получение тегов по бренду и родительскому ID
  async getTagsByBrand(brandId, parentId = null) {
    try {
      const params = {};
      if (parentId !== null && parentId !== undefined) {
        params.parentId = parentId;
      }
      
      console.log(`Получение тегов для бренда ${brandId}, родительский ID: ${parentId}`);
      const response = await api.get(`${API_PREFIX}/by-brand/${brandId}`, { 
        params,
        retryCount: 0 // Добавляем счетчик попыток для обработчика ошибок
      });
      
      console.log(`Получены теги:`, response.data);
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      return handleApiError(error, 'Не удалось загрузить теги');
    }
  },

  // Получение полного дерева тегов для бренда
  async getTagTree(brandId) {
    try {
      console.log(`Получение дерева тегов для бренда ${brandId}`);
      const response = await api.get(`${API_PREFIX}/tree/${brandId}`, {
        retryCount: 0
      });
      return response.data;
    } catch (error) {
      console.warn('Не удалось загрузить дерево тегов, загружаем только корневые теги', error);
      // В случае ошибки возвращаем пустой массив, чтобы не ломать UI
      return [];
    }
  },

  // Получение тега по ID
  async getTagById(tagId) {
    try {
      console.log(`Получение тега с ID: ${tagId}`);
      const response = await api.get(`${API_PREFIX}/${tagId}`, {
        retryCount: 0
      });
      return response.data;
    } catch (error) {
      return handleApiError(error, 'Не удалось загрузить тег');
    }
  },

  // Обновление тега
  async updateTag(tagId, tagData) {
    try {
      console.log(`Обновление тега ${tagId}:`, tagData);
      const response = await api.put(`${API_PREFIX}/${tagId}`, tagData, {
        retryCount: 0
      });
      console.log('Тег успешно обновлен:', response.data);
      return response.data;
    } catch (error) {
      return handleApiError(error, 'Не удалось обновить тег');
    }
  },

  // Удаление тега
  async deleteTag(tagId) {
    try {
      console.log(`Удаление тега с ID: ${tagId}`);
      const response = await api.delete(`${API_PREFIX}/${tagId}`, {
        retryCount: 0
      });
      console.log('Тег успешно удален:', response.data);
      return response.data;
    } catch (error) {
      return handleApiError(error, 'Не удалось удалить тег');
    }
  }
};

export default tagService;
