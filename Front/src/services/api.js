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
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('Добавлен токен авторизации в заголовок');
    } else {
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
    const originalRequest = error.config;
    const authStore = useAuthStore();

    if (error.response.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise(function(resolve, reject) {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          originalRequest.headers['Authorization'] = 'Bearer ' + token;
          return apiClient(originalRequest);
        }).catch(err => {
          return Promise.reject(err);
        });
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
        console.error('Не удалось восстановить сессию, выход из системы.', refreshError);
        authStore.logout();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
