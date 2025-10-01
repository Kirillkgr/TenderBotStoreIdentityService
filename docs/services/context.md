# Context (стабильный API)

Минимальный, но полный набор для работы с контекстом. Не планируется менять до BL‑10.

## GET /auth/v1/memberships

- Требует авторизации (`Authorization: Bearer ...` или cookie accessToken)
- Выход: массив `{ membershipId, masterId, masterName, brandId?, brandName?, locationId? }`

## POST /auth/v1/context/switch

- Вход: `{ membershipId, brandId?, locationId? }`
- Выход: `{ accessToken }` — новый токен с клеймами контекста
- Правила:
    - `membershipId` обязательно и принадлежит текущему пользователю
    - `brandId`/`locationId` должны соответствовать выбранному membership

## Dev‑заголовок

- Профиль `dev` поддерживает `X-Master-Id` (для локальной отладки)
