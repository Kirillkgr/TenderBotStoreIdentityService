# Frontend — аутентификация и обновление токена

## Логин

- Запрос: `POST /auth/v1/login`
- Клиент отправляет Basic в `Authorization`.
- Ответ: профиль пользователя, backend устанавливает HttpOnly cookie `refreshToken` (`SameSite=None`, `Secure` в prod).
- Если есть cookie `cart_token`, backend объединит гостевую корзину с пользовательской.

## Обновление токена

- Запрос: `POST /auth/v1/refresh`
- Браузер автоматически отправляет HttpOnly cookie `refreshToken` при условии:
  - `withCredentials: true` на фронте
  - CORS-разрешение точного Origin
  - В dev по HTTP нужно `app.cookie.secure=false` на сервере
- Ответ содержит новый `accessToken` и Set-Cookie для нового `refreshToken`.

## Logout

- `DELETE /auth/v1/logout` — ревокация access/refresh, сервер возвращает Set-Cookie на очистку.
- `DELETE /auth/v1/logout/all/{username}` — ревокация всех токенов пользователя (админ).

## Работа с заголовками

- `Authorization: Bearer <accessToken>` добавляется ко всем защищённым вызовам после логина/refresh.
- Храните accessToken в памяти приложения (не в localStorage).

## Ошибки

- 400 — неверный запрос/валидация
- 401 — неавторизован (истёк/неверен access)
- 403 — недостаточно прав (RBAC)
- 409 — конфликты (например, дубль сущности)
- Формат см. `docs/services/errors.md`
