# Единый каталог ошибок (стабильные правила)

Фиксируем минимальный, предсказуемый набор кодов и их источники.

## Коды и правила маппинга

- 200 OK — запрос выполнен
- 201 Created — ресурс создан (регистрация, создание бренда/товара и т.д.)
- 400 Bad Request — ошибка валидации/параметров
- 401 Unauthorized — нет/невалидный токен
  - Источник: `SecurityConfig.authenticationEntryPoint` или fallback `AuthenticationException` в
    `GlobalExceptionHandler`.
- 403 Forbidden — недостаточно прав или нет tenant‑контекста
  - Источники:
    - RBAC: `RbacGuard.requireOwnerOrAdmin()` выбрасывает `AccessDeniedException` → `GlobalExceptionHandler` => 403.
    - Нет `masterId` при аутентифицированном запросе: `ContextEnforcementFilter` => 403.
- 404 Not Found — ресурс не найден в вашем контексте (маскировка чужих сущностей)
  - Источники: `ResourceNotFoundException` и `jakarta.persistence.EntityNotFoundException` → `GlobalExceptionHandler` =>
    404.
- 409 Conflict — конфликт уникальности (username/email/brand name)
- 422 Unprocessable Entity — бизнес‑ограничение (например, перемещение группы к другому бренду)

## Единый ответ об ошибке

Рекомендуемый формат (минимально необходимый и стабильный):

```json
{
  "status": 403,
  "message": "Недостаточно прав"
}
```

Допустимо расширение:

```json
{
  "timestamp": "2025-10-01T12:34:56Z",
  "status": 404,
  "error": "Not Found",
  "message": "Ресурс не найден",
  "path": "/auth/v1/brands/123"
}
```

Фактическую сериализацию делает `GlobalExceptionHandler`.

## Где реализовано

- `security/SecurityConfig` — 401/403 через `authenticationEntryPoint` и `accessDeniedHandler`.
- `tenant/ContextEnforcementFilter` — 403 при отсутствии контекста у аутентифицированного запроса.
- `security/RbacGuard` — бросает `AccessDeniedException` (403) для админ‑операций.
- `exception/GlobalExceptionHandler` — единое маппирование: 400/401/403/404/409/422/500.

## Примеры сценариев

- __401__: `GET /auth/v1/brands` без токена → 401.
- __403 (RBAC)__: `POST /auth/v1/brands` с ролью `CLIENT` → 403.
- __403 (контекст)__: `GET /auth/v1/brands` с токеном без `masterId` → 403.
- __404__: `GET /auth/v1/brands/{id}` в чужом `master` → 404.

Тесты: `ErrorCodesStandardizationTest` проверяет 401/403/404.
