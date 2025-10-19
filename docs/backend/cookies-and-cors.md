# Cookies и CORS

## Cookies

- `refreshToken` — HttpOnly, `SameSite=None`, домен задаётся `server.servlet.session.cookie.domain`.
- Флаг `Secure` регулируется `app.cookie.secure` (prod: true, dev HTTP: false).
- Очистка: `CookieService.buildClearRefreshCookies()` и `buildClearSessionCookies()` выставляют max-age=0 (оба варианта — с domain и без).

## CORS

- Разрешённые источники задаются в конфигурации CORS (см. `CorsConfig`).
- Важно: origin должен совпадать точно (без завершающего `/`).
- Для работы с cookies фронт должен использовать `withCredentials: true`.

## Частые проблемы

- 400 на `/auth/v1/refresh` в dev: браузер не отправляет Secure cookie по HTTP. Решение: `app.cookie.secure=false`.
- Cookie не приходит на фронт: проверьте `SameSite=None` и `withCredentials: true`.
- Блокировка CORS: проверьте, что origin фронта добавлен без завершающего слеша.
