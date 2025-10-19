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
- Админ → [Клиенты (мастер-область, бренд и дата последнего заказа)](wiki/admin-clients)
- Frontend → [Страница остатков (Stock)](wiki/frontend-inventory-stock)

### Сервисы (стабильные)

- [Auth](wiki/auth)
- [Context](wiki/context)
- [RBAC](wiki/rbac)
- [Public Menu](wiki/menu)
- [Health](wiki/health)
- [Ошибки и коды](wiki/errors)

---

## Frontend

- [Оглавление и практики фронтенда](wiki/frontindex)
- [Аутентификация и обновление токена](wiki/frontend-auth)
- [Контекст на фронте](wiki/frontend-context)
- [Страница остатков (Stock)](wiki/frontend-inventory-stock)
- [Уведомления и long‑poll](wiki/frontend-notifications-longpoll)
- [CRUD по инвентарю](wiki/frontend-inventory-crud)

## Backend (дополнения)

- [Swagger / OpenAPI (группы и аннотации)](wiki/swagger)
- [Cookies и CORS](wiki/cookies-and-cors)
- [TenantContext и публичные пути](wiki/tenant-context)
- [ACL заказов](wiki/order-acl)
