# TenderBotStore Identity Service — Документация

Добро пожаловать! Здесь описаны основы сервиса идентификации и многотенантности TenderBotStore.

- Что такое многотенантность и зачем она нужна
- Как устроены сущности (ER‑схема)
- Как работает контекст (membership/master/brand/location)
- Как выглядят токены и заголовки
- Как добавлять миграции БД (Liquibase)

Если вы новичок — читайте страницы по порядку. Каждая тема короткая и максимально простая.

## Быстрые ссылки

- Архитектура → [ER‑схема](wiki/er-schema)
- Архитектура → [Многотенантность и контекст](wiki/multitenancy)
- Сервисы → [RBAC (роли и доступ)](wiki/rbac)
- Backend → [Гайд по миграциям (Liquibase)](wiki/migrations)
- Frontend → [Контекст на фронте](wiki/frontend-context)
- Инвентарь → [Склады и ингредиенты](wiki/inventory)
- Инвентарь → [Поставки и остатки](wiki/supplies-and-stock)

### Сервисы (стабильные)

- [Auth](wiki/auth)
- [Context](wiki/context)
- [RBAC](wiki/rbac)
- [Public Menu](wiki/menu)
- [Health](wiki/health)
- [Ошибки и коды](wiki/errors)
