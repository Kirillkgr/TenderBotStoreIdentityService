# TenderBotStore — Multi‑tenant E‑commerce Platform (SaaS)
[![CI](https://github.com/Kirillkgr/TenderBotStoreIdentityService/actions/workflows/test-backend-frontend-ci.yml/badge.svg)](https://github.com/Kirillkgr/TenderBotStoreIdentityService/actions/workflows/test-backend-frontend-ci.yml)

## Обзор

TenderBotStore — это многоарендная платформа e‑commerce (каталог/корзина/чекаут) с разграничением доступа по контексту
Master/Brand/Location и ролям Membership (RBAC). Репозиторий содержит backend (Java/Spring Boot), frontend (Vue 3),
инфраструктуру (Liquibase, Docker), и документацию (MkDocs).

Ключевые возможности (по дорожной карте):

- Многотенантность (BL‑1): контекст в JWT, изоляция данных по `masterId/brandId/locationId`.
- Магазин (BL‑8): публичный каталог, корзина, чекаут.
- Инвентарь/себестоимость/модификаторы — по следующим этапам.

### Документация

- [Wiki (главная)](wiki/home)
- Навигатор (исходники): [docs/index.md](docs/index.md)
- [RBAC (BL‑2)](wiki/rbac)
- [Многотенантность/контекст](wiki/multitenancy)
- [Инвентарь/склад](wiki/inventory)
- [Поставки и остатки](wiki/supplies-and-stock)

#### Документация в Wiki (онлайн)

- Главная Wiki: [Home](wiki/home)
- Внутренние ссылки указывают на страницы Wiki (без расширений .md), например: [Context](wiki/multitenancy).
### Документация API (Swagger/OpenAPI)

- После запуска приложения откройте Swagger UI:
    - http://localhost:8081/swagger-ui.html (или http://localhost:8081/swagger-ui)
    - Контекст попадает в токен после `POST /auth/v1/context/switch`.
  - Подробнее: [Context](wiki/multitenancy).

- Заголовки:
    - `Authorization: Bearer <accessToken>` — обязательно для защищённых ручек `/auth/v1/**`.
    - `X-Membership-Id: <id>` — ставит фронт для трассировки (полезно, но не обязателен).
    - `X-Master-Id: <id>` — dev‑фолбэк для локальной отладки (только профиль dev).

- Миграции (Liquibase):
    - Мастер‑файл: `src/main/resources/db/changelog/db.changelog-master.xml`.
    - Новые файлы кладём по дате и подключаем через `<include/>`.
  - Best practices и примеры: [Migrations](wiki/migrations).

- Фронт и контекст:
    - Login → `GET /auth/v1/memberships` → выбор → `POST /auth/v1/context/switch` → новый `accessToken`.
    - E2E smoke тест (полностью мокнутый): `Front/tests/AuthContextE2ESmoke.spec.js`.
  - Подробнее: [Frontend Context](wiki/frontend-context).

Если вы новичок, начните с `docs/index.md` — там есть короткий путеводитель.
