import api from './api';
import {useAuthStore} from '@/store/auth';

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
  // Архив групп по бренду
  async getGroupArchiveByBrand(brandId) {
    try {
      const response = await api.get(`${API_PREFIX}/archive`, {params: {brandId}, retryCount: 0});
      return Array.isArray(response.data) ? response.data : (response.data?.data ?? []);
    } catch (error) {
      return handleApiError(error, 'Не удалось загрузить архив тегов');
    }
  },
  async getGroupArchiveByBrandPaged(brandId, {page = 0, size = 25, sort = 'archivedAt,desc'} = {}) {
    try {
      const response = await api.get(`${API_PREFIX}/archive/paged`, {
        params: {brandId, page, size, sort},
        retryCount: 0
      });
      return response.data; // ожидаем Page DTO {content, totalElements, ...}
    } catch (error) {
      return handleApiError(error, 'Не удалось загрузить архив тегов (пагинация)');
    }
  },
  async restoreGroupFromArchive(archiveId, targetParentId = null) {
    try {
      const config = targetParentId != null ? {params: {targetParentId}, retryCount: 0} : {retryCount: 0};
      const response = await api.post(`${API_PREFIX}/archive/${archiveId}/restore`, null, config);
      return response.data;
    } catch (error) {
      if (error?.response?.status === 404) {
        return Promise.reject(new Error('Запись архива тега не найдена (возможно, уже восстановлена или удалена). Обновите список.'));
      }
      return handleApiError(error, 'Не удалось восстановить тег из архива');
    }
  },
  async deleteGroupArchive(archiveId) {
    try {
      const response = await api.delete(`${API_PREFIX}/archive/${archiveId}`, {retryCount: 0});
      return response.data;
    } catch (error) {
      return handleApiError(error, 'Не удалось удалить запись архива тега');
    }
  },
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

  // Единый апдейт: бренд/родитель/имя одним запросом
  async updateFull(tagId, payload) {
    try {
      const body = {
        name: payload?.name ?? null,
        parentId: payload?.parentId ?? null,
        brandId: payload?.brandId ?? null
      };
      const response = await api.put(`${API_PREFIX}/${tagId}/full`, body, { retryCount: 0 });
      return response.data;
    } catch (error) {
      return handleApiError(error, 'Не удалось обновить тег');
    }
  },

  // Смена бренда у тега (и его поддерева)
  async changeGroupBrand(tagId, brandId) {
    try {
      const response = await api.patch(`${API_PREFIX}/${tagId}/brand`, null, {
        params: { brandId },
        retryCount: 0
      });
      return response.data;
    } catch (error) {
      return handleApiError(error, 'Не удалось сменить бренд у тега');
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

  // Получение полного дерева тегов для бренда (быстрый бэкенд-эндпоинт)
  async getTagTree(brandId) {
    try {
      const response = await api.get(`${API_PREFIX}/tree/${brandId}/full`, { retryCount: 0 });
      return Array.isArray(response.data) ? response.data : (response.data?.data ?? []);
    } catch (error) {
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
      // ВНИМАНИЕ: Бэкенд разделяет операции: переименование (PUT с query) и перенос (PATCH /move)
      // Этот метод сохранён для обратной совместимости, но рекомендуется вызывать renameTag/moveTag явно.
      let last;
      if (typeof tagData.name === 'string' && tagData.name.trim().length > 0) {
        last = await this.renameTag(tagId, tagData.name);
      }
      if (tagData.parentId !== undefined && tagData.parentId !== null) {
        last = await this.moveTag(tagId, tagData.parentId);
      }
      return last;
    } catch (error) {
      return handleApiError(error, 'Не удалось обновить тег');
    }
  },

  // Переименование тега (совместимо с бэкендом: PUT /{id}?name=)
  async renameTag(tagId, name) {
    try {
      const response = await api.put(`${API_PREFIX}/${tagId}`, null, {
        params: { name },
        retryCount: 0
      });
      return response.data;
    } catch (error) {
      return handleApiError(error, 'Не удалось переименовать тег');
    }
  },

  // Перемещение тега (совместимо с бэкендом: PATCH /{id}/move?parentId=)
  async moveTag(tagId, parentId) {
    try {
      const params = {};
      if (parentId !== undefined && parentId !== null) params.parentId = parentId;
      const response = await api.patch(`${API_PREFIX}/${tagId}/move`, null, {
        params,
        retryCount: 0
      });
      return response.data;
    } catch (error) {
      return handleApiError(error, 'Не удалось переместить тег');
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
