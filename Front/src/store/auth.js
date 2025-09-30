import {defineStore} from 'pinia';
import * as authService from '../services/authService';
import router from '../router';
import {useCartStore} from './cart';

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
        // Кол-во непрочитанных (приближённо): берём с сервера при логине
        unreadCount: 0,
        // Контекст (membership/master/brand/location) для tenant-aware запросов
        memberships: [],
        membershipId: null,
        masterId: null,
        brandId: null,
        locationId: null,
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
            // unreadCount обновляется через long-poll

            // После логина получаем доступные memberships
            try {
                const res = await authService.getMemberships();
                const list = Array.isArray(res?.data) ? res.data : [];
                this.memberships = list;
                // Если доступен только один membership — выбираем автоматически
                if (list.length === 1) {
                    await this.selectMembership(list[0]);
                } else if (list.length > 1) {
                    // Сообщаем UI открыть модалку выбора контекста
                    try {
                        window.dispatchEvent(new CustomEvent('open-context-modal'));
                    } catch (_) {
                    }
                }
            } catch (e) {
                // игнорируем: возможно, пользователь без membership (приглашение не принято и т.п.)
            }
        },

        async register(credentials) {
            const response = await authService.register(credentials);
            const { accessToken, ...userData } = response.data;

            this.setAccessToken(accessToken);
            this.setUser(userData);
            // unreadCount обновляется через long-poll
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

            // Очистим корзину и локальные идентификаторы корзины, чтобы не таскать гостевую корзину между сессиями
            try {
                const cart = useCartStore();
                cart.clearCart();
            } catch (_) {
            }
            try {
                localStorage.removeItem('cart_scope_id');
            } catch (_) {
            }
            try {
                localStorage.removeItem('cart_token');
            } catch (_) {
            }
            try {
                localStorage.removeItem('cart_brand_id');
            } catch (_) {
            }

            this.setUser(null);
            this.setAccessToken(null);
            this.unreadCount = 0;
            this.clearMembership();
            try { localStorage.removeItem(USER_STORAGE_KEY); } catch(_) {}

            await router.push('/');
            this.isLoggingOut = false;
        },

        // Клиентское очищение состояния без запроса на бэкенд (например, если refresh 4xx)
        async clearSession() {
            this.setUser(null);
            this.setAccessToken(null);
            this.unreadCount = 0;
            this.clearMembership();
            try { localStorage.removeItem(USER_STORAGE_KEY); } catch(_) {}
            // Те же очистки корзины при клиентском сбросе
            try {
                const cart = useCartStore();
                cart.clearCart();
            } catch (_) {
            }
            try {
                localStorage.removeItem('cart_scope_id');
            } catch (_) {
            }
            try {
                localStorage.removeItem('cart_token');
            } catch (_) {
            }
            try {
                localStorage.removeItem('cart_brand_id');
            } catch (_) {
            }
            await router.push('/');
        },

        // Попытка восстановить сессию из refresh cookie
        async restoreSession() {
            // помечаем, что идёт восстановление, чтобы роутер мог учитывать это при редиректах
            this.isRestoringSession = true;
            try {
                // Явный вызов refresh через axios-клиент (withCredentials уже активен в apiClient)
                const resp = await authService.refresh();
                const {accessToken} = resp?.data || {};
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
                } catch (_) {
                }
                // Попытка подтянуть memberships для UI (без автоселекта)
                try {
                    const res = await authService.getMemberships();
                    this.memberships = Array.isArray(res?.data) ? res.data : [];
                    // Если ранее пользователь выбирал контекст — восстановим его
                    try {
                        const savedId = localStorage.getItem('selected_membership_id');
                        if (savedId && !this.membershipId) {
                            const m = this.memberships.find(x => String(x.membershipId || x.id) === String(savedId));
                            if (m) {
                                await this.selectMembership(m);
                            }
                        }
                    } catch (_) {
                    }
                } catch (_) {}
                return true;
            } catch (e) {
                await this.clearSession();
                return false;
            } finally {
                this.isRestoringSession = false;
                // unreadCount обновляется через long-poll
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
        },

        // Выбор membership: сохраняем контекст и переключаем JWT на бэке
        async selectMembership(m) {
            if (!m) return;
            const payload = {
                membershipId: m.membershipId || m.id,
                brandId: m.brandId || null,
                locationId: m.locationId || null,
            };
            // Вызов switch выдаёт новый accessToken с клеймами контекста
            const resp = await authService.switchContext(payload);
            const newAccessToken = resp?.data?.accessToken;
            if (newAccessToken) {
                this.setAccessToken(newAccessToken);
            }
            // Сохраняем выбранный контекст в store (для отрисовки и заголовков)
            this.membershipId = payload.membershipId || null;
            this.masterId = m.masterId || null;
            this.brandId = m.brandId || null;
            this.locationId = m.locationId || null;
            try {
                localStorage.setItem('selected_membership_id', String(this.membershipId));
            } catch (_) {
            }
        },

        // Очистка контекста на клиенте (без server-side действий)
        clearMembership() {
            this.membershipId = null;
            this.masterId = null;
            this.brandId = null;
            this.locationId = null;
            this.memberships = [];
            try {
                localStorage.removeItem('selected_membership_id');
            } catch (_) {
            }
        }
    },
});
