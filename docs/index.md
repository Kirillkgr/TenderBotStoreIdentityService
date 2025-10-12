# TenderBotStore Identity Service — Документация

Добро пожаловать! Здесь описаны основы сервиса идентификации и многотенантности TenderBotStore.

- Что такое многотенантность и зачем она нужна
- Как устроены сущности (ER‑схема)
- Как работает контекст (membership/master/brand/location)
- Как выглядят токены и заголовки
- Как добавлять миграции БД (Liquibase)

Если вы новичок — читайте страницы по порядку. Каждая тема короткая и максимально простая.

## Быстрые ссылки

- Архитектура → [ER‑схема](https://github.com/Kirillkgr/TenderBotStoreIdentityService/wiki/ER-Schema)
- Архитектура → [Многотенантность и контекст](https://github.com/Kirillkgr/TenderBotStoreIdentityService/wiki/Context)
- Сервисы → [RBAC (роли и доступ)](https://github.com/Kirillkgr/TenderBotStoreIdentityService/wiki/RBAC)
- Backend → [Гайд по миграциям (Liquibase)](https://github.com/Kirillkgr/TenderBotStoreIdentityService/wiki/Migrations)
- Frontend → [Контекст на фронте](https://github.com/Kirillkgr/TenderBotStoreIdentityService/wiki/Frontend-Context)
- Инвентарь → [Склады и ингредиенты](https://github.com/Kirillkgr/TenderBotStoreIdentityService/wiki/Inventory)
- Инвентарь → [Поставки и остатки](https://github.com/Kirillkgr/TenderBotStoreIdentityService/wiki/Supplies-and-Stock)

### Сервисы (стабильные)

- [Auth](https://github.com/Kirillkgr/TenderBotStoreIdentityService/wiki/Auth)
- [Context](https://github.com/Kirillkgr/TenderBotStoreIdentityService/wiki/Context)
- [RBAC](https://github.com/Kirillkgr/TenderBotStoreIdentityService/wiki/RBAC)
- [Public Menu](https://github.com/Kirillkgr/TenderBotStoreIdentityService/wiki/Menu)
- [Health](https://github.com/Kirillkgr/TenderBotStoreIdentityService/wiki/Health)
- [Ошибки и коды](https://github.com/Kirillkgr/TenderBotStoreIdentityService/wiki/Errors)
