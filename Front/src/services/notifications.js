import apiClient from './api';
import {useToast} from 'vue-toastification';

// Simple long-poll notifications client with ACK and subscription API
class NotificationsClient {
    constructor() {
        this.since = 0;
        this.running = false;
        this.subscribers = new Set();
        this.toast = null;
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
                const resp = await apiClient.get('/notifications/longpoll', {
                    params: {since: this.since, maxBatch: 50, timeoutMs: 25000},
                    validateStatus: (s) => (s >= 200 && s < 300) || s === 204 || s === 201,
                });
                if (resp.status === 204 || resp.status === 201) {
                    // Нет событий — корректный idle-ответ, просто продолжаем
                    await new Promise(r => setTimeout(r, 150));
                    continue;
                }
                const data = resp.data;
                const events = Array.isArray(data?.events) ? data.events : [];
                const nextSince = Number(data?.nextSince ?? this.since) || this.since;
                const hasMore = !!data?.hasMore;

                if (events.length > 0) {
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
                // network/server error: backoff a bit
                await new Promise(r => setTimeout(r, 1500));
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
