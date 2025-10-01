# Identity Service for TenderBotStore

## Обзор

`identity-service` — это микросервис, отвечающий за аутентификацию и авторизацию пользователей в экосистеме
TenderBotStore. Он предоставляет REST API для регистрации, входа в систему, обновления и отзыва токенов доступа.

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
