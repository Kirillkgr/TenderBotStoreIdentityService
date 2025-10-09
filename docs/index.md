# TenderBotStore Identity Service — Документация

Добро пожаловать! Здесь описаны основы сервиса идентификации и многотенантности TenderBotStore.

- Что такое многотенантность и зачем она нужна
- Как устроены сущности (ER‑схема)
- Как работает контекст (membership/master/brand/location)
- Как выглядят токены и заголовки
- Как добавлять миграции БД (Liquibase)

Если вы новичок — читайте страницы по порядку. Каждая тема короткая и максимально простая.

## Быстрые ссылки

- Архитектура → [ER‑схема](architecture/er-schema)
- Архитектура → [Многотенантность и контекст](architecture/multitenancy)
- Сервисы → [RBAC (роли и доступ)](services/rbac)
- Backend → [Гайд по миграциям (Liquibase)](backend/migrations)
- Frontend → [Контекст на фронте](frontend/context)

### Сервисы (стабильные)

- [Auth](services/auth)
- [Context](services/context)
- [RBAC](services/rbac)
- [Public Menu](services/menu)
- [Health](services/health)
- [Ошибки и коды](services/errors)
