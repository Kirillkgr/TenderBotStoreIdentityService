// Специальная обработка для long-poll: никогда не блокируем этот эндпоинт анти-штормом
function isLongpollUrl(url = '') {
    try {
        return url.startsWith('/notifications/longpoll');
    } catch (_) {
        return false;
    }
}

import axios from 'axios';
import {useAuthStore} from '../store/auth';

const DEBUG_HTTP = import.meta.env.VITE_DEBUG_HTTP === 'true';

let base = import.meta.env.VITE_API_BASE_URL;
try {
    const malformed = !base || base === '/' ? false : /^(https?:\/\/)$/i.test(base.trim());
    if (!base || malformed) {
        base = '/';
    }
} catch (_) {
    base = '/';
}

const apiClient = axios.create({
    baseURL: base,
  withCredentials: true, // Разрешаем отправку и получение cookies
});

// Одноразовый стартовый лог диагностики HTTP-клиента
(function bootstrapHttpDiagnostics() {
  try {
      const baseEnv = import.meta.env.VITE_API_BASE_URL;
    const cookiesEnabled = navigator.cookieEnabled;
    const hasDocCookie = typeof document !== 'undefined' && !!document.cookie;
    if (DEBUG_HTTP) {
      console.log('[HTTP] bootstrap', {
        baseURL: base,
          baseEnv,
        withCredentials: true,
        cookiesEnabled,
        documentCookiePresent: hasDocCookie,
      });
    }
  } catch (e) {
    if (DEBUG_HTTP) console.warn('[HTTP] bootstrap diagnostics failed', e);
  }
})();

// ---- Анти-шторм: временная блокировка проблемных эндпоинтов и троттлинг рефреша ----
const DISABLED_KEY = 'disabled_endpoints'; // sessionStorage, чтобы сбрасывалось при перезагрузке страницы
const DISABLE_TTL_MS = 5 * 60 * 1000; // 5 минут
const REFRESH_INTERVAL_MS = 1000; // не чаще одного раза в секунду

function readDisabledMap() {
    try {
        const raw = sessionStorage.getItem(DISABLED_KEY);
        const obj = raw ? JSON.parse(raw) : {};
        const now = Date.now();
        // очистка протухших записей
        let changed = false;
        for (const k of Object.keys(obj)) {
            if (!obj[k] || obj[k] < now) {
                delete obj[k];
                changed = true;
            }
        }
        if (changed) sessionStorage.setItem(DISABLED_KEY, JSON.stringify(obj));
        return obj;
    } catch (_) {
        return {};
    }
}

function isEndpointDisabled(url) {
    try {
        const map = readDisabledMap();
        return !!map[url];
    } catch (_) {
        return false;
    }
}

function disableEndpoint(url, ttlMs = DISABLE_TTL_MS) {
    try {
        const map = readDisabledMap();
        map[url] = Date.now() + (ttlMs || DISABLE_TTL_MS);
        sessionStorage.setItem(DISABLED_KEY, JSON.stringify(map));
    } catch (_) {
    }
}

let lastRefreshTs = 0;

// Перехватчик для добавления токена авторизации в каждый запрос
apiClient.interceptors.request.use(
    async (config) => {
    if (DEBUG_HTTP) {
      try {
        console.log(`Отправка запроса: ${config.method.toUpperCase()} ${config.url}`, {
          url: config.url,
          method: config.method,
          baseURL: config.baseURL,
          withCredentials: config.withCredentials,
        });
      } catch (_) {}
    }
    const authStore = useAuthStore();
    const token = authStore.accessToken;

        // Удалено: идентификаторы корзины больше не передаются заголовками; используем httpOnly cookie cart_token

      // Блокируем запросы к ранее помеченным как проблемные эндпоинтам (на 5 минут)
      try {
          const reqUrl = config.url || '';
          const isAuthEndpoint = reqUrl.startsWith('/auth/v1/');
          // long-poll никогда не блокируем
          if (!isAuthEndpoint && !isLongpollUrl(reqUrl) && isEndpointDisabled(reqUrl)) {
              const err = new Error('Функция временно недоступна. Повторите попытку позже.');
              err.code = 'ENDPOINT_DISABLED';
              err.isTemporarilyDisabled = true;
              return Promise.reject(err);
          }
      } catch (_) {
      }

    const url = config.url || '';
    const isPublicAuthEndpoint = url.startsWith('/auth/v1/login')
      || url.startsWith('/auth/v1/register')
      || url.startsWith('/auth/v1/checkUsername')
      || url.startsWith('/auth/v1/refresh');
        const isPublicMenuEndpoint = url.startsWith('/menu/');
        const isPublicCartEndpoint = url.startsWith('/cart');

    const hasBasicHeader = !!config.headers?.Authorization && /^Basic\s/i.test(config.headers.Authorization);

        // Удалено: контекст больше не передаём через X-* заголовки (используем httpOnly cookie ctx)

        // Если запрос защищённый, а токена пока нет, но идёт восстановление — коротко подождём
        if (!isPublicAuthEndpoint && !isPublicMenuEndpoint && !isPublicCartEndpoint && !hasBasicHeader && !token && authStore?.isRestoringSession) {
            try {
                if (DEBUG_HTTP) console.log('[HTTP] waiting restoreSession before request', url);
                await new Promise(r => setTimeout(r, 300));
            } catch (_) {
            }
        }
        const effToken = authStore?.accessToken || token;
        // Явная отсечка: если это защищённый эндпоинт, токена нет и не публичный auth —
        // не шлём запрос на сервер, а просим пользователя авторизоваться
        if (!isPublicAuthEndpoint && !isPublicMenuEndpoint && !isPublicCartEndpoint && !hasBasicHeader && !effToken) {
            try {
                window.dispatchEvent(new Event('open-login-modal'));
            } catch (_) {
            }
            const err = new Error('Требуется авторизация');
            err.code = 'NO_AUTH';
            return Promise.reject(err);
        }
        if (!isPublicAuthEndpoint && !hasBasicHeader && effToken) {
            config.headers.Authorization = `Bearer ${effToken}`;
      if (DEBUG_HTTP) console.log('Добавлен токен авторизации в заголовок');
        } else if (!token && !hasBasicHeader && !isPublicAuthEndpoint && !isPublicMenuEndpoint && !isPublicCartEndpoint) {
      if (DEBUG_HTTP) console.warn('Токен авторизации отсутствует (используем HttpOnly cookie, если они установлены)');
    }

        // Удалено: X-Membership-Id / X-Master-Id не используются
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
    if (DEBUG_HTTP) {
      console.log(`Получен ответ от ${response.config.url}:`, {
        status: response.status,
        data: response.data
      });
    }
    return response;
  },
  async (error) => {
    try {
      if (DEBUG_HTTP) console.warn('Ошибка ответа', {
        url: error.config?.url,
        status: error.response?.status,
        data: error.response?.data,
      });
    } catch (_) {}
    const originalRequest = error.config || {};
    const authStore = useAuthStore();

    const status = error.response?.status;
    const url = originalRequest.url || '';
    const isPublicAuthEndpoint = url.startsWith('/auth/v1/login')
      || url.startsWith('/auth/v1/register')
      || url.startsWith('/auth/v1/checkUsername')
      || url.startsWith('/auth/v1/refresh');

    const hasBasicHeader = !!originalRequest.headers?.Authorization && /^Basic\s/i.test(originalRequest.headers.Authorization);

    // Если мы выходим или помечено пропустить рефреш — не делаем refresh
    const skipOnce = (() => { try { return localStorage.getItem('skip_refresh_once') === '1'; } catch(_) { return false; } })();
    if (authStore.isLoggingOut || skipOnce) {
        // Очистим флаг единовременного пропуска рефреша, чтобы последующие сессии работали штатно
        try {
            if (skipOnce) localStorage.removeItem('skip_refresh_once');
        } catch (_) {
        }
      return Promise.reject(error);
    }

      // Не пытаемся рефрешить, если это публичные auth-запросы, Basic или статус не 401
      // ВАЖНО: разрешаем рефреш даже когда accessToken отсутствует — тогда используем refresh-cookie
      if (status !== 401 || isPublicAuthEndpoint || isPublicMenuEndpoint || isPublicCartEndpoint || hasBasicHeader) {
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
      originalRequest._retryCount = (originalRequest._retryCount || 0) + 1;
    isRefreshing = true;

    try {
        // Троттлинг рефреша: не чаще 1 раза в секунду
        const now = Date.now();
        const diff = now - lastRefreshTs;
        if (diff < REFRESH_INTERVAL_MS) {
            await new Promise(r => setTimeout(r, REFRESH_INTERVAL_MS - diff));
        }
      const response = await apiClient.post('/auth/v1/refresh');
      const { accessToken, ...userData } = response.data;

      authStore.setAccessToken(accessToken);
      authStore.setUser(userData);

      apiClient.defaults.headers.common['Authorization'] = 'Bearer ' + accessToken;
      originalRequest.headers['Authorization'] = 'Bearer ' + accessToken;
        lastRefreshTs = Date.now();

      processQueue(null, accessToken);
        // Повторяем исходный запрос один раз
        return await apiClient(originalRequest);

    } catch (refreshError) {
      processQueue(refreshError, null);
      console.error('Не удалось восстановить сессию (куки refresh отсутствует или просрочен). Локальная очистка.');
      // Важно: не вызываем server-side logout, просто чистим локальную сессию
      await authStore.clearSession();
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);

// Дополнительный перехватчик: если после успешного refresh повторный запрос всё равно заваливается —
// помечаем эндпоинт временно недоступным на 5 минут, чтобы избежать каскада.
apiClient.interceptors.response.use(
    (resp) => resp,
    async (error) => {
        try {
            const originalRequest = error.config || {};
            const status = error.response?.status;
            const url = originalRequest.url || '';
            const isAuthEndpoint = url.startsWith('/auth/v1/');
            const isLp = isLongpollUrl(url);
            // Если это не auth и мы уже делали _retry, а ответ снова 401/403/5xx — отключаем эндпоинт на 5 минут
            // НО: никогда не отключаем long-poll — иначе сломаем фоновую доставку событий
            if (!isAuthEndpoint && !isLp && originalRequest._retry && (status === 401 || status === 403 || (status >= 500 && status <= 599))) {
                disableEndpoint(url);
                const err = new Error('Функция временно недоступна. Повторите попытку через несколько минут.');
                err.code = 'ENDPOINT_DISABLED_AFTER_RETRY';
                err.isTemporarilyDisabled = true;
                return Promise.reject(err);
            }
        } catch (_) {
        }
        return Promise.reject(error);
    }
);

export default apiClient;
