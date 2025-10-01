# Auth (стабильный API)

Короткое и стабильное описание ручек аутентификации. Эти ручки не планируется менять до BL‑10.

## POST /auth/v1/register

- Вход: `{ username, email, password, roleNames[] }`
- Выход: `{ username, accessToken }` (+ refreshToken в cookie, если настроено)
- Ошибки: 400 (валидация), 409 (username/email занят)

## POST /auth/v1/login

- Заголовок: `Authorization: Basic base64(username:password)`
- Выход: `{ username, accessToken }` + HttpOnly cookie `refreshToken`
- Ошибки: 401 (неверные креды)

## POST /auth/v1/refresh

- Вход: cookie `refreshToken`
- Выход: `{ accessToken }` + установка нового `refreshToken` (ротация)
- Ошибки: 403 (refresh недействителен)

## POST /auth/v1/revoke

- Параметр: `token=<accessToken>`
- Выход: 200

## POST /auth/v1/revoke-all

- Параметр: `username=<name>` (ADMIN)
- Выход: 200
