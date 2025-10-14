import {defineStore} from 'pinia';

export const useNotificationsStore = defineStore('notifications', {
    state: () => ({
        // Непрочитанные по заказам: { [orderId]: count }
        unreadByOrder: {},
        // Текущий открытый чат (заказ)
        activeOrderId: null,
        queuedCount: 0,
        queuedByOrder: {}, // { [orderId]: true }
        // Дедупликация пользовательских тостов (клиентские подсказки)
        cartAnonPromptShown: false, // показать "Зарегистрируйтесь, чтобы оформить заказ" только один раз за сессию
        cartAddedByProduct: {}, // { [productId]: true } — тост "товар добавлен" один раз для каждого товара
        // nav-dot (красная точка на аватарке клиента)
        clientNavDot: false,
    }),
    getters: {
        hasUnreadByOrder: (state) => (orderId) => {
            const id = Number(orderId);
            return id && state.unreadByOrder[id] > 0;
        },
        countByOrder: (state) => (orderId) => {
            const id = Number(orderId);
            return id ? (state.unreadByOrder[id] || 0) : 0;
        },
        hasAnyUnread: (state) => Object.values(state.unreadByOrder || {}).some(v => (v | 0) > 0),
        hasQueued: (state) => (state.queuedCount | 0) > 0,
        isQueued: (state) => (orderId) => !!state.queuedByOrder?.[Number(orderId)],
        isActive: (state) => (orderId) => {
            const id = Number(orderId);
            return !!id && Number(state.activeOrderId) === id;
        },
        wasAnonPromptShown: (state) => !!state.cartAnonPromptShown,
        wasProductAddedToastShown: (state) => (productId) => !!state.cartAddedByProduct?.[Number(productId)],
        hasClientNavDot: (state) => !!state.clientNavDot,
    },
    actions: {
        markUnread(orderId, inc = 1) {
            const id = Number(orderId);
            if (!id) return;
            const cur = this.unreadByOrder[id] || 0;
            this.unreadByOrder = {...this.unreadByOrder, [id]: cur + (Number(inc) || 1)};
        },
        clearOrder(orderId) {
            const id = Number(orderId);
            if (!id) return;
            if (!this.unreadByOrder[id]) return;
            const copy = {...this.unreadByOrder};
            delete copy[id];
            this.unreadByOrder = copy;
        },
        clearAll() {
            this.unreadByOrder = {};
            this.activeOrderId = null;
            this.queuedCount = 0;
            this.queuedByOrder = {};
        },
        setActive(orderId) {
            const id = Number(orderId);
            this.activeOrderId = id || null;
        },
        clearActive() {
            this.activeOrderId = null;
        },
        setQueuedCount(n) {
            const v = Number(n);
            this.queuedCount = Number.isFinite(v) && v >= 0 ? v : 0;
        },
        markQueued(orderId) {
            const id = Number(orderId);
            if (!id) return;
            if (this.queuedByOrder[id]) return;
            this.queuedByOrder = {...this.queuedByOrder, [id]: true};
            this.queuedCount = Object.keys(this.queuedByOrder).length;
        },
        clearQueued(orderId) {
            const id = Number(orderId);
            if (!id || !this.queuedByOrder[id]) return;
            const copy = {...this.queuedByOrder};
            delete copy[id];
            this.queuedByOrder = copy;
            this.queuedCount = Object.keys(this.queuedByOrder).length;
        },
        resetQueuedFromList(orders) {
            const map = {};
            (orders || []).forEach(o => {
                if (o && String(o.status).toUpperCase() === 'QUEUED') map[Number(o.id)] = true;
            });
            this.queuedByOrder = map;
            this.queuedCount = Object.keys(map).length;
        },

        // --- Дедуп тостов корзины ---
        shouldShowAnonCartPrompt() {
            if (this.cartAnonPromptShown) return false;
            this.cartAnonPromptShown = true;
            return true;
        },
        shouldShowAddedProductToast(productId) {
            const id = Number(productId);
            if (!id) return false;
            if (this.cartAddedByProduct?.[id]) return false;
            this.cartAddedByProduct = {...(this.cartAddedByProduct || {}), [id]: true};
            return true;
        },
        resetCartToasts() {
            this.cartAnonPromptShown = false;
            this.cartAddedByProduct = {};
        }
        ,
        // --- nav-dot на аватарке клиента ---
        markClientNavDot() {
            this.clientNavDot = true;
        },
        clearClientNavDot() {
            this.clientNavDot = false;
        },
    },
});
