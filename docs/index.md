# TenderBotStore Identity Service — Документация

Добро пожаловать! Здесь описаны основы сервиса идентификации и многотенантности TenderBotStore.

- Что такое многотенантность и зачем она нужна
- Как устроены сущности (ER‑схема)
- Как работает контекст (membership/master/brand/location)
- Как выглядят токены и заголовки
- Как добавлять миграции БД (Liquibase)

Если вы новичок — читайте страницы по порядку. Каждая тема короткая и максимально простая.

## Быстрые ссылки

- Архитектура → [ER‑схема](architecture/er-schema.md)
- Архитектура → [Многотенантность и контекст](architecture/multitenancy.md)
- Сервисы → [RBAC (роли и доступ)](services/rbac.md)
- Backend → [Гайд по миграциям (Liquibase)](backend/migrations.md)
- Frontend → [Контекст на фронте](frontend/context.md)

### Сервисы (стабильные)

- [Auth](services/auth.md)
- [Context](services/context.md)
- [RBAC](services/rbac.md)
- [Public Menu](services/menu.md)
- [Health](services/health.md)
- [Ошибки и коды](services/errors.md)
