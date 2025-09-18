import axios from 'axios';
import { useAuthStore } from '../store/auth';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true, // Разрешаем отправку и получение cookies
});

// Перехватчик для добавления токена авторизации в каждый запрос
apiClient.interceptors.request.use(
  (config) => {
    console.log(`Отправка запроса: ${config.method.toUpperCase()} ${config.url}`, config);
    const authStore = useAuthStore();
    const token = authStore.accessToken;

    const url = config.url || '';
    const isPublicAuthEndpoint = url.startsWith('/auth/v1/login')
      || url.startsWith('/auth/v1/register')
      || url.startsWith('/auth/v1/checkUsername')
      || url.startsWith('/auth/v1/refresh');

    const hasBasicHeader = !!config.headers?.Authorization && /^Basic\s/i.test(config.headers.Authorization);

    if (!isPublicAuthEndpoint && !hasBasicHeader && token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('Добавлен токен авторизации в заголовок');
    } else if (!token && !hasBasicHeader && !isPublicAuthEndpoint) {
      console.warn('Токен авторизации отсутствует');
    }
    return config;
  },
  (error) => {
    console.error('Ошибка в перехватчике запроса:', error);
    return Promise.reject(error);
  }
);

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Перехватчик для обработки ответов
apiClient.interceptors.response.use(
  (response) => {
    console.log(`Получен ответ от ${response.config.url}:`, {
      status: response.status,
      data: response.data
    });
    return response;
  }, // Успешные ответы логируем и пробрасываем дальше
  async (error) => {
    const originalRequest = error.config || {};
    const authStore = useAuthStore();

    const status = error.response?.status;
    const url = originalRequest.url || '';
    const isPublicAuthEndpoint = url.startsWith('/auth/v1/login')
      || url.startsWith('/auth/v1/register')
      || url.startsWith('/auth/v1/checkUsername')
      || url.startsWith('/auth/v1/refresh');

    const hasBasicHeader = !!originalRequest.headers?.Authorization && /^Basic\s/i.test(originalRequest.headers.Authorization);

    // Не пытаемся рефрешить, если это публичные auth-запросы, Basic, нет accessToken или статус не 401
    if (status !== 401 || isPublicAuthEndpoint || hasBasicHeader || !authStore.accessToken) {
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise(function(resolve, reject) {
        failedQueue.push({ resolve, reject });
      }).then(token => {
        originalRequest.headers['Authorization'] = 'Bearer ' + token;
        return apiClient(originalRequest);
      }).catch(err => Promise.reject(err));
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      const response = await apiClient.post('/auth/v1/refresh');
      const { accessToken, ...userData } = response.data;

      authStore.setAccessToken(accessToken);
      authStore.setUser(userData);

      apiClient.defaults.headers.common['Authorization'] = 'Bearer ' + accessToken;
      originalRequest.headers['Authorization'] = 'Bearer ' + accessToken;

      processQueue(null, accessToken);
      return apiClient(originalRequest);

    } catch (refreshError) {
      processQueue(refreshError, null);
      console.error('Не удалось восстановить сессию (куки refresh отсутствует или просрочен). Выход.', refreshError);
      authStore.logout();
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);

export default apiClient;
