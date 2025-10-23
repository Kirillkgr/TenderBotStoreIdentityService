import apiClient from './api';
import {useToast} from 'vue-toastification';
import {useAuthStore} from '../store/auth';
import {useNotificationsStore} from '../store/notifications';

// Simple long-poll notifications client with ACK and subscription API
class NotificationsClient {
    constructor() {
        this.since = 0;
        this.running = false;
        this.subscribers = new Set();
        this.toast = null;
        // Экспоненциальный бэкофф (1s -> 2s -> 4s ... -> 10m)
        this.initialBackoffMs = 1000;
        this.maxBackoffMs = 10 * 60 * 1000;
        this.backoffMs = this.initialBackoffMs;
    }

    initToast() {
        if (!this.toast) this.toast = useToast();
    }

    subscribe(fn) {
        this.subscribers.add(fn);
        return () => this.subscribers.delete(fn);
    }

    async start() {
        if (this.running) return;
        this.running = true;
        this.initToast();
        // Сброс бэкоффа на старте/после перезагрузки страницы
        this.backoffMs = this.initialBackoffMs;
        // restore since from localStorage
        try {
            const v = localStorage.getItem('lp_since');
            if (v) this.since = Number(v) || 0;
        } catch {
        }
        this.loop();
    }

    stop() {
        this.running = false;
    }

    async loop() {
        // Сервер по умолчанию ждёт до 60 секунд, можно не передавать timeoutMs
        while (this.running) {
            try {
                // Сервер сам управляет таймаутом/пакетированием. Клиент передаёт только 'since'.
                const resp = await apiClient.get('/notifications/longpoll', {
                    params: {since: this.since},
                });
                // Любой успешный ответ — сбрасываем бэкофф
                this.backoffMs = this.initialBackoffMs;
                // Обновляем индикатор непрочитанных, если сервер прислал значение
                try {
                    const authStore = useAuthStore();
                    const uc = Number(resp?.data?.unreadCount);
                    if (!Number.isNaN(uc) && uc >= 0) authStore.unreadCount = uc;
                } catch (_) {
                }

                if (resp.status === 204 || resp.status === 201) {
                    // Нет событий — корректный idle-ответ, просто продолжаем
                    await new Promise(r => setTimeout(r, 150));
                    continue;
                }
                const data = resp.data;
                const events = Array.isArray(data?.events) ? data.events : [];
                const nextSince = Number(data?.nextSince ?? this.since) || this.since;
                const hasMore = !!data?.hasMore;
                // Ещё раз обновим непрочитанные из полезной нагрузки, если есть
                try {
                    const authStore = useAuthStore();
                    const uc2 = Number(data?.unreadCount);
                    if (!Number.isNaN(uc2) && uc2 >= 0) authStore.unreadCount = uc2;
                } catch (_) {
                }

                if (events.length > 0) {
                    // Отметим по заказам непрочитанные
                    try {
                        const nStore = useNotificationsStore();
                        const authStore = useAuthStore();
                        const roles = Array.isArray(authStore?.roles) ? authStore.roles : [];
                        const isStaff = roles.includes('ADMIN') || roles.includes('OWNER');
                        for (const e of events) {
                            const t = String(e?.type || '').toUpperCase();
                            const isMessage = /MESSAGE/.test(t);
                            const isClientMsg = t.includes('CLIENT');
                            const isStaffMsg = t.includes('ADMIN') || t.includes('STAFF') || t.includes('COURIER');

                            if (e && isMessage && e.orderId) {
                                // если чат по заказу открыт — не увеличиваем непрочитанные
                                if (!nStore.isActive(e.orderId)) {
                                    // Для персонала считаем непрочитанными только сообщения клиента
                                    if (isStaff && isClientMsg) {
                                        nStore.markUnread(e.orderId, 1);
                                    }
                                    // Для клиента считаем непрочитанными ЛЮБОЕ сообщение (сервер может не указывать роль отправителя)
                                    if (!isStaff) {
                                        nStore.markUnread(e.orderId, 1);
                                    }
                                }
                                // Нав-дот на аватарке при ЛЮБОМ входящем сообщении, если чат не активен
                                try {
                                    if (!nStore.isActive(e.orderId)) nStore.markClientNavDot();
                                } catch (_) {
                                }
                            } else if (e && t === 'NEW_ORDER' && e.orderId && isStaff) {
                                // новый заказ: перезагружаем список и проигрываем короткий звук
                                nStore.markQueued(e.orderId);
                            } else if (e && e.type === 'ORDER_STATUS_CHANGED' && e.orderId) {
                                const ns = String(e.newStatus || '').toUpperCase();
                                if (isStaff) {
                                    if (ns === 'QUEUED') nStore.markQueued(e.orderId); else nStore.clearQueued(e.orderId);
                                } else {
                                    // Клиенту: показать nav-dot на аватарке при смене статуса
                                    try {
                                        nStore.markClientNavDot();
                                    } catch (_) {
                                    }
                                }
                            }
                            try {
                                if (typeof console !== 'undefined') console.debug('[LP] event', t, e.orderId);
                            } catch (_) {
                            }
                        }
                    } catch (_) {
                    }
                    // dispatch events
                    for (const e of events) {
                        this.notify(e);
                    }
                    // ack
                    try {
                        await apiClient.post('/notifications/longpoll/ack', {lastReceivedId: nextSince});
                        this.since = nextSince;
                        try {
                            localStorage.setItem('lp_since', String(this.since));
                        } catch {
                        }
                    } catch (ackErr) {
                        console.warn('ACK failed', ackErr);
                    }
                }

                // Немедленно продолжаем, если сервер сигнализирует о наличии ещё событий
                if (hasMore) continue;
            } catch (e) {
                // Ошибка сети/сервера/авторизации: экспоненциальный бэкофф
                // Если бэкофф превысил лимит — прекращаем попытки до перезагрузки страницы
                if (this.backoffMs > this.maxBackoffMs) {
                    this.running = false;
                    try {
                        if (this.toast) this.toast.error('Уведомления недоступны. Обновите страницу для повторной попытки.');
                    } catch (_) {}
                    break;
                }
                await new Promise(r => setTimeout(r, this.backoffMs));
                // Увеличиваем бэкофф в 2 раза (с ограничением)
                this.backoffMs = Math.min(this.backoffMs * 2, this.maxBackoffMs + 1);
            }
            // small delay between polls to avoid tight loop after empty response
            await new Promise(r => setTimeout(r, 150));
        }
    }

    notify(evt) {
        for (const fn of this.subscribers) {
            try {
                fn(evt);
            } catch (e) {
                console.error('subscriber error', e);
            }
        }
        // default UX:
        try {
            if (!this.toast) return;
            if (evt.type === 'ORDER_STATUS_CHANGED') {
                this.toast.success(`Заказ #${evt.orderId}: статус ${evt.oldStatus} → ${evt.newStatus}`);
            } else if (evt.type === 'COURIER_MESSAGE') {
                this.toast.info(`Сообщение по заказу #${evt.orderId}: ${evt.text}`);
            } else if (evt.type === 'CLIENT_MESSAGE') {
                this.toast.info(`Клиент написал по заказу #${evt.orderId}: ${evt.text}`);
            }
        } catch {
        }
    }
}

let singleton;

export function getNotificationsClient() {
    if (!singleton) singleton = new NotificationsClient();
    return singleton;
}
