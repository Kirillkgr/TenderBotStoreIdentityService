# Frontend — уведомления и long‑poll

## Зачем

Long‑poll используется для доставки событий (например, новые заказы/сообщения) без постоянного обновления страницы.

## Требования

- Использовать HTTP‑клиент с `withCredentials: true`, чтобы отправлялись cookies/авторизация.
- CORS на backend должен разрешать точный Origin фронта (без завершающего `/`).
- Пользователь должен быть аутентифицирован, и контекст (master/brand) должен быть установлен.

## Рекомендации реализации

- Таймаут запроса — короче серверного таймаута, чтобы успевать переподключаться.
- Экспоненциальный backoff при сетевых ошибках.
- Отменять предыдущий long‑poll перед запуском нового.

```ts
async function startLongPoll(signal: AbortSignal) {
  while (!signal.aborted) {
    try {
      const res = await api.get('/notifications/longpoll', { signal, timeout: 65000 });
      if (res.data) handleEvents(res.data);
    } catch (e) {
      if (signal.aborted) break;
      await wait(1500); // backoff
    }
  }
}
```

## Частые проблемы

- Куки не уходят — проверьте `withCredentials: true` и `SameSite=None` у cookies.
- 401/403 — отсутствует аутентификация или недостаточно прав (RBAC), проверьте access/refresh и контекст.
- Дубли событий — убедитесь, что параллельно не выполняются несколько long‑poll.
