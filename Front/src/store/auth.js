import { defineStore } from 'pinia';
import * as authService from '../services/authService';
import router from '../router';

export const useAuthStore = defineStore('auth', {
    state: () => ({
        // Токены и данные пользователя хранятся только в памяти, не в localStorage
        user: null,
        accessToken: null,
        // Флаг, чтобы показать, что мы пытаемся восстановить сессию после перезагрузки
        isRestoringSession: false,
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
            this.user = userData;
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
            try {
                await authService.logout();
            } catch (error) {
                console.error('Ошибка при выходе из системы на сервере:', error);
            }

            this.setUser(null);
            this.setAccessToken(null);

            await router.push('/');
        },
    },
});
