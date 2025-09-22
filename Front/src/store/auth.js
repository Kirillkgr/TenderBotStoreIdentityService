import {defineStore} from 'pinia';
import * as authService from '../services/authService';
import router from '../router';

const USER_STORAGE_KEY = 'user_data';

export const useAuthStore = defineStore('auth', {
    state: () => ({
        // Токены и данные пользователя хранятся только в памяти, не в localStorage
        user: null,
        accessToken: null,
        // Флаг, чтобы показать, что мы пытаемся восстановить сессию после перезагрузки
        isRestoringSession: false,
        // Флаг, чтобы не запускать refresh во время выхода
        isLoggingOut: false,
    }),

    getters: {
        isAuthenticated: (state) => !!state.accessToken,
        currentUser: (state) => state.user,
    },

    actions: {
        setAccessToken(accessToken) {
            this.accessToken = accessToken;
        },

        setUser(userData) {
            // Если пришёл null — явная очистка пользователя
            if (userData === null) {
                this.user = null;
                try { localStorage.removeItem(USER_STORAGE_KEY); } catch (_) {}
                return;
            }

            // Если пришёл пустой объект (типичный кейс ответа refresh без полей пользователя)
            // — НЕ перетираем текущие данные пользователя и localStorage.
            if (typeof userData === 'object' && userData && Object.keys(userData).length === 0) {
                return;
            }

            // Нормализация: поддержка внешних названий аватара (google picture / generic avatar / imageUrl)
            const normalized = {...(typeof userData === 'object' ? userData : {})};
            if (!normalized.avatarUrl) {
                const extAvatar = normalized.picture || normalized.avatar || normalized.imageUrl || normalized.photoURL || normalized.photoUrl;
                if (extAvatar) normalized.avatarUrl = extAvatar;
            }

            // Иначе обновляем и сохраняем
            this.user = normalized;
            try {
                localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(normalized));
            } catch (_) {
            }
        },

        async login(credentials) {
            const response = await authService.login(credentials);
            const { accessToken, ...userData } = response.data;

            this.setAccessToken(accessToken);
            this.setUser(userData);
        },

        async register(credentials) {
            const response = await authService.register(credentials);
            const { accessToken, ...userData } = response.data;

            this.setAccessToken(accessToken);
            this.setUser(userData);
        },

        async checkUsername(username) {
            try {
                await authService.checkUsername(username);
                return { available: true };
            } catch (error) {
                return { available: false, message: error.response?.data?.message || 'Этот логин уже занят.' };
            }
        },

        async logout() {
            this.isLoggingOut = true;
            try {
                // Передаём текущий accessToken в тело запроса, как ожидает бэкенд
                const token = this.accessToken;
                await authService.logout(token);
            } catch (error) {
                console.error('Ошибка при выходе из системы на сервере:', error);
            }

            // Маркер, чтобы на следующей загрузке и в перехватчике не делать refresh
            try { localStorage.setItem('skip_refresh_once', '1'); } catch(_) {}

            this.setUser(null);
            this.setAccessToken(null);
            try { localStorage.removeItem(USER_STORAGE_KEY); } catch(_) {}

            await router.push('/');
            this.isLoggingOut = false;
        },

        // Клиентское очищение состояния без запроса на бэкенд (например, если refresh 4xx)
        async clearSession() {
            this.setUser(null);
            this.setAccessToken(null);
            try { localStorage.removeItem(USER_STORAGE_KEY); } catch(_) {}
            await router.push('/');
        },

        // Попытка восстановить сессию из refresh cookie
        async restoreSession() {
            // помечаем, что идёт восстановление, чтобы роутер мог учитывать это при редиректах
            this.isRestoringSession = true;
            try {
                // Явный вызов refresh: сервер читает httpOnly refreshToken из cookie и вернёт новый accessToken
                const refreshResp = await fetch(`${import.meta.env.VITE_API_BASE_URL}/auth/v1/refresh`, {
                    method: 'POST',
                    credentials: 'include',
                });
                if (!refreshResp.ok) {
                    // 4xx/5xx — refresh невалиден
                    await this.clearSession();
                    return false;
                }
                const data = await refreshResp.json();
                const { accessToken } = data || {};
                if (!accessToken) {
                    await this.clearSession();
                    return false;
                }
                this.setAccessToken(accessToken); // только токен; профиль берём из localStorage (hydrateFromStorage)
                // Если профиль в localStorage отсутствует — подтягиваем с сервера
                try {
                    const raw = localStorage.getItem('user_data');
                    if (!raw) {
                        await this.fetchProfile();
                    }
                } catch (_) {}
                return true;
            } catch (e) {
                await this.clearSession();
                return false;
            } finally {
                this.isRestoringSession = false;
            }
        },

        // Гидратация пользовательских данных из localStorage (без токена)
        hydrateFromStorage() {
            try {
                const raw = localStorage.getItem(USER_STORAGE_KEY);
                if (raw) {
                    const parsed = JSON.parse(raw);
                    // setUser также обновит localStorage, это безопасно
                    this.setUser(parsed);
                }
            } catch (_) { /* ignore */ }
        }
        ,
        async fetchProfile() {
            try {
                const me = await authService.getCurrentUser();
                if (me && typeof me === 'object') {
                    // Пускаем через setUser, чтобы сработала нормализация avatarUrl
                    this.setUser(me);
                }
            } catch (_) {
                // игнорируем: отсутствие авторизации/ошибка сети
            }
        }
    },
});
