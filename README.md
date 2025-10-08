# TenderBotStore — Multi‑tenant E‑commerce Platform (SaaS)

## Обзор

TenderBotStore — это многоарендная платформа e‑commerce (каталог/корзина/чекаут) с разграничением доступа по контексту
Master/Brand/Location и ролям Membership (RBAC). Репозиторий содержит backend (Java/Spring Boot), frontend (Vue 3),
инфраструктуру (Liquibase, Docker), и документацию (MkDocs).

Ключевые возможности (по дорожной карте):

- Многотенантность (BL‑1): контекст в JWT, изоляция данных по `masterId/brandId/locationId`.
- RBAC на Membership (BL‑2): роли `OWNER/ADMIN/CASHIER/COOK/CLIENT` и проверки доступа по активному membership.
- Магазин (BL‑8): публичный каталог, корзина, чекаут.
- Инвентарь/себестоимость/модификаторы — по следующим этапам.

### Документация

- Wiki: https://github.com/Kirillkgr/TenderBotStoreIdentityService/wiki
- Навигатор (исходники): [docs/index.md](docs/index.md)
- RBAC (BL‑2): [docs/services/rbac.md](docs/services/rbac.md)
- Многотенантность/контекст: [docs/architecture/multitenancy.md](docs/architecture/multitenancy.md)

#### Документация в Wiki (онлайн)

- Главная Wiki: https://github.com/Kirillkgr/TenderBotStoreIdentityService/wiki/Home
- Все внутренние ссылки в Wiki ведут на отрендеренные страницы (без расширения .md).

### Документация API (Swagger/OpenAPI)

- После запуска приложения откройте Swagger UI:
    - http://localhost:8081/swagger-ui.html (или http://localhost:8081/swagger-ui)
- Если UI недоступен — проверьте зависимость SpringDoc в `pom.xml`:
    - `org.springdoc:springdoc-openapi-starter-webmvc-ui` (для Spring MVC)
- Аннотации `@Operation` добавлены в контроллеры каталога, контекста, аутентификации, корзины, медиа, заказов и пр.,
  включая пометки по ролям (RBAC).
- Актуальную структуру и ссылки также смотрите в Wiki (см. выше).

## Технологический стек

- **Java 21**: Основной язык программирования.
- **Spring Boot**: Фреймворк для создания микросервисов.
- **Spring Security**: Обеспечение безопасности и управление доступом.
- **PostgreSQL**: Реляционная база данных для хранения информации о пользователях.
- **Apache Kafka**: Платформа для обмена сообщениями между сервисами.
- **JWT (JSON Web Tokens)**: Для создания токенов доступа.
- **Maven**: Система управления зависимостями и сборки проекта.
- **Docker**: Контейнеризация для упрощения развертывания.

## Сборка и запуск

### Требования

- JDK 21
- Maven 3.x
- Docker
- Docker Compose

### Инструкции

1. **Склонируйте репозиторий:**

   ```bash
   git clone <URL репозитория>
   cd TenderBotStoreIdentityService
   ```

2. **Запустите зависимости с помощью Docker Compose:**

   В корневой директории проекта выполните команду:

   ```bash
   docker-compose up -d
   ```

   Эта команда запустит контейнеры с PostgreSQL и Kafka.

3. **Соберите проект с помощью Maven:**

   ```bash
   mvn clean install
   ```

4. **Запустите сервис:**

   ```bash
   java -jar target/identity-service-0.0.1-SNAPSHOT.jar
   ```

   Сервис будет доступен по адресу `http://localhost:8081`.

## Конфигурация

Основные параметры конфигурации находятся в файле `src/main/resources/application.yml`:

- **Порт сервера**: `server.port`
- **Настройки базы данных**: `spring.datasource`
- **Настройки Kafka**: `spring.kafka`
- **Секретный ключ JWT**: `jwt.secret` (рекомендуется изменять для производственной среды)

## API Endpoints

Подробную информацию о доступных эндпоинтах можно найти в Swagger UI по адресу `http://localhost:8081/swagger-ui.html`
после запуска сервиса.

## Tenant Context (JWT) и контекст переключения

Для изоляции данных между владельцами (master/brand/location) backend использует контекст в JWT.

- В access-token добавлены клеймы:
    - `membershipId`, `masterId`, опционально `brandId`, `locationId`.
- Вход по логину/refresh выдаёт обычный access-token. Для выбора рабочего контекста используйте эндпоинт переключения.

### Переключение контекста

`POST /auth/v1/context/switch`

Body:

```json
{
  "membershipId": 123,            
  "brandId": 456,                 
  "locationId": 789               
}
```

- `membershipId` — обязателен; должен принадлежать текущему пользователю.
- `brandId`/`locationId` — опциональные. Если указаны, должны совпадать с brand/pickup, связанными с переданным
  membership.

Response:

```json
{ "accessToken": "<JWT-with-context>" }
```

Frontend-поток:

1. Login → получить accessToken (без контекста).
2. Пользователь выбирает контекст (membership/brand/location) → вызвать `/auth/v1/context/switch`.
3. Сохранить новый accessToken и использовать его для защищённых запросов. Контекст подтягивается на backend из клеймов
   JWT автоматически.

### Dev fallback

Для локальной отладки в профиле `dev` доступен заголовок `X-Master-Id`, который устанавливает masterId на время запроса.
В production этот механизм отключён и использоваться не должен.

## Многотенантность и контекст — кратко (DoD 1.8)

Ниже — самое важное для понимания изоляции данных и контекста. Полные версии смотрите в docs/.

- ER‑картинка (упрощённо):
    - `MasterAccount` (владелец) → `Brand` → (`GroupTag`/`Product`/`Order`)
    - `User` и его `UserMembership` связывают пользователя с `MasterAccount` (и опционально `Brand`/`PickupPoint`).
    - Подробнее: `docs/architecture/er-schema.md`.

- Формат токена (JWT клеймы контекста):
  ```json
  {
    "sub": "username",
    "iat": 1710000000,
    "exp": 1710003600,
    "membershipId": 11,
    "masterId": 101,
    "brandId": 1001,
    "locationId": 0
  }
  ```
    - Контекст попадает в токен после `POST /auth/v1/context/switch`.
    - Подробнее: `docs/architecture/multitenancy.md`.

- Заголовки:
    - `Authorization: Bearer <accessToken>` — обязательно для защищённых ручек `/auth/v1/**`.
    - `X-Membership-Id: <id>` — ставит фронт для трассировки (полезно, но не обязателен).
    - `X-Master-Id: <id>` — dev‑фолбэк для локальной отладки (только профиль dev).

- Миграции (Liquibase):
    - Мастер‑файл: `src/main/resources/db/changelog/db.changelog-master.xml`.
    - Новые файлы кладём по дате и подключаем через `<include/>`.
    - Best practices и примеры: `docs/backend/migrations.md`.

- Фронт и контекст:
    - Login → `GET /auth/v1/memberships` → выбор → `POST /auth/v1/context/switch` → новый `accessToken`.
    - E2E smoke тест (полностью мокнутый): `Front/tests/AuthContextE2ESmoke.spec.js`.
    - Подробнее: `docs/frontend/context.md`.

Если вы новичок, начните с `docs/index.md` — там есть короткий путеводитель.
