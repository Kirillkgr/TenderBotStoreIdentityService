# TenderBotStoreIdentityService — Аудит файлов и их ответственность

Ниже приведён подробный обзор структуры репозитория и роли каждого файла/папки. Описания сгруппированы по директориям.

## Корень проекта `/`
- **`.gitattributes`** — настройка атрибутов Git (нормализация окончаний строк и т.д.).
- **`.gitignore`** — исключения для Git.
- **`.github/`** — GitHub Actions и конфиги автоматизации (см. раздел ниже).
- **`.idea/`** — метаданные IDE (IntelliJ), не используются приложением.
- **`.mvn/`** — служебные файлы Maven Wrapper.
- **`Dockerfile`** — многоэтапная сборка backend (maven build + лёгкий JRE-рантайм).
- **`Front/`** — фронтенд-приложение (Vue 3, Vite): исходники, тесты, сборка.
- **`README.md`** — обзор проекта и навигация по документации.
- **`docker-compose.yml`** — прод-подобный docker-compose (postgres + бекенд + фронт, секреты через env).
- **`docker-compose-test.yml`** — локальная тестовая среда: Postgres + Kafka/Zookeeper.
- **`docs/`** — документация MkDocs (архитектура, бэкенд, фронтенд, сервисы).
- **`mkdocs.yml`** — конфиг MkDocs (сборка документации).
- **`mvnw`, `mvnw.cmd`** — Maven Wrapper для кроссплатформенного запуска.
- **`package-lock.json`** — корневой lock-файл npm (для вспомогательных скриптов).
- **`pom.xml`** — основной Maven-проект: зависимости, плагины, профили.
- **`src/`** — бэкенд исходники и тесты (Java/Spring Boot).
- **`starts3.sh`** — вспомогательный скрипт для S3 (dev/ops задачи).
- **`store.md`** — дополнительная документация по проекту/домены.
- **`target/`** — артефакты сборки Maven (игнорируется в VCS).
- **`updateCert/`** — утилиты для генерации/обновления TLS-сертификатов.
- **`updateCert.sh`** — скрипт автоматизации обновления сертификата.

## CI/CD `.github/workflows/`
- **`CI-CD-PROJECT.yml`** — пайплайн CI/CD: сборка, публикация контейнеров/артефактов.
- **`docs-wiki-pr.yml`** — автосборка/деплой документации и wiki по PR.
- **`test-backend-frontend-ci.yml`** — CI для прогонки тестов backend и frontend.
- **`update-cert.yml`** — автоматизация обновления сертификатов (cron/trigger).

## Docker/Compose
- **`Dockerfile`** —
  - Этап `build`: `maven:3.9.6-eclipse-temurin-21`, кеширование зависимостей, сборка JAR с `-DskipTests`.
  - Этап `run`: `eclipse-temurin:21-jre-alpine`, копирование `app.jar`, `EXPOSE 9900`, `ENTRYPOINT`.
- **`docker-compose.yml`** — сервисы `postgres`, `backend`, `frontend`, переменные окружения, маунты сертификатов/nginx.
- **`docker-compose-test.yml`** — сервисы `postgres` (порт 5433->5432), `zookeeper`, `kafka` с необходимыми переменными.

## Backend: Maven `pom.xml`
- **Зависимости**: Spring Boot (web, validation, data-jpa, security, mail, oauth2-client), Postgres, Liquibase, Kafka, JWT (jjwt), Lombok, Swagger (springdoc), AWS SDK S3, Thumbnailator, TwelveMonkeys ImageIO.
- **Управление зависимостями**: BOM Spring Boot и Testcontainers.
- **Плагины**: `jacoco-maven-plugin`, `spring-boot-maven-plugin`, `maven-compiler-plugin`, `maven-surefire-plugin` (настройка таймаутов и включений тестов).
- **Профили**: `smoke-acl` для прогонки smoke-тестов с тегами.

## Backend: ресурсы `src/main/resources/`
- **`application.yml`** — конфигурация: порт сервера, JPA/Hibernate, datasource (локально `localhost:5433/identity_db`), Liquibase, multipart, OAuth2 Google (client-id/secret/redirect-uri), JWT (секрет и TTL), CORS-домены фронта, S3 (Yandex Object Storage), Springdoc (Swagger UI пути).
- **`db/`** — миграции Liquibase.
  - **`changelog/db.changelog-master.xml`** — мастер-файл, включает миграции из подпапок.
  - **`changelog/2025/09/*.xml`** — изменения схемы/данных за 2025-09:
    - `23-01-changelog.xml`
    - `23-02-changelog.xml`
    - `23-03-changelog.xml`
    - `24-01-changelog.xml`
    - `27-01-changelog.xml`
    - `29-01-changelog.xml`
    - `30-01-changelog.xml`
  - **`changelog/2025/10/*.xml`** — миграции за 2025-10 (4 файла).
- **`logback-spring.xml`** — конфиг логирования (паттерны/уровни для Spring).

## Backend: код `src/main/java/kirillzhdanov/identityservice/`
- **`IdentityServiceApplication.java`** — точка входа Spring Boot.

### `config/`
- **`BrandContextInterceptor.java`** — перехватчик запросов для внедрения контекста бренда.
- **`CorsConfig.java`** — настройка CORS (разрешённые источники/методы/заголовки).
- **`CorsProperties.java`** — бин со свойствами CORS.
- **`DatabaseInitializer.java`** — инициализация БД (seed/демо-данные/референсные данные при старте).
- **`OpenApiConfig.java`** — конфигурация Springdoc/Swagger (группы, метаданные).
- **`WebConfig.java`** — общие веб-настройки (регистрация интерсепторов и т.п.).

### `controller/` — REST-контроллеры
- **`AccountLinkController.java`** — привязка аккаунтов/провайдеров.
- **`AuthController.java`** — аутентификация: login/refresh/logout, выдача токенов.
- **`BotController.java`** — эндпоинты бота/интеграции.
- **`BrandController.java`** — CRUD брендов и поиск.
- **`CartController.java`** — корзина: операции с позициями, подсчёт, очистка.
- **`ContextController.java`** — переключение контекста (master/brand/location), получение текущего.
- **`GroupTagController.java`** — дерево групп-тегов, CRUD и архивирование.
- **`HealthController.java`** — health-check.
- **`KitchenController.java`** — ручки кухни (операции для поваров/столовой зоны).
- **`MediaController.java`** — загрузка/выдача медиа, привязка к продуктам/тегам.
- **`MembershipController.java`** — операции с членствами и ролями.
- **`MenuController.java`** — публичное меню/каталог.
- **`OrderControler.java`** — вспомогательные ручки для заказов (опечатка в имени файла сохранена).
- **`ProductController.java`** — CRUD продуктов, архив/восстановление, фильтры.
- **`StaffController.java`** — управление персоналом/отделами.
- **`TokenValidationController.java`** — проверка/валидация токенов.
- **`UserController.java`** — профиль пользователя, регистрация, обновление.
- `admin/`:
  - **`AdminClientController.java`** — административные операции с клиентами.
  - **`AdminOrderController.java`** — административные операции по заказам.
- `checkout/`:
  - **`CheckoutController.java`** — оформление заказа, расчёты, подтверждение.
- `inventory/`:
  - **`IngredientController.java`** — CRUD ингредиентов.
  - **`PackagingController.java`** — CRUD упаковки.
  - **`StockController.java`** — остатки/движения.
  - **`SupplierController.java`** — поставщики.
  - **`SupplyController.java`** — поставки и позиции поставок.
  - **`UnitController.java`** — единицы измерения.
  - **`WarehouseController.java`** — склады.
- `order/`:
  - **`OrderController.java`** — CRUD/поиск/статусы заказов, сообщения/ревью.
- `pickup/`:
  - **`PickupPointController.java`** — точки самовывоза.
- `profile/`:
  - **`ProfileAddressController.java`** — адреса доставки профиля.
- `publicapi/`:
  - **`PublicPickupPointController.java`** — публичная выдача точек самовывоза.

### `dto/` — транспортные модели запросов/ответов
- Базовые:
  - `AddressDto.java`, `AvatarUploadResponse.java`, `BotRegistrationRequest.java`, `BrandDto.java`,
    `ContextSwitchRequest.java`, `ContextSwitchResponse.java`, `CtxSetRequest.java`,
    `EmailVerificationRequest.java`, `EmailVerifiedResponse.java`, `JwtUserDetailsResponse.java`,
    `LoginRequest.java`, `LoginResponse.java`, `MembershipDto.java`, `MessageResponse.java`,
    `ProductImageRef.java`, `TagImageRef.java`, `TokenRefreshRequest.java`, `TokenRefreshResponse.java`,
    `UpdateUserRequest.java`, `UserRegistrationRequest.java`, `UserResponse.java` — стандартные DTO по названию.
- `client/`: `ClientDto.java`, `ClientProjection.java` — данные клиента и проекции.
- `group/`: CRUD/ответы по групп-тегам.
- `inventory/`: CRUD ингредиентов/складов/единиц, корректировки остатков, поставки.
- `menu/`: публичные ответы по бренду/товарам/группам.
- `order/`: DTO заказа, позиции, обновление статуса, сообщения курьера.
- `product/`: CRUD/ответы продукта и архива.
- `staff/`: CRUD сотрудников/отделов, пагинация, детали пользователя для staff.

### `exception/`
- **`BadRequestException.java`**, **`ResourceAlreadyExistsException.java`**, **`ResourceNotFoundException.java`**, **`TokenRefreshException.java`** — доменные исключения.
- **`GlobalExceptionHandler.java`** — глобальный обработчик, нормализует ответы об ошибках.

### `googleOAuth2/`
- **`CustomOidcUserService.java`** — кастомизация загрузки OIDC-пользователя.
- **`GoogleOAuth2Service.java`** — логика OAuth2/Google, обмен кодов, профили, связка с пользователями.
- **`OAuth2LoginFailureHandler.java`**, **`OAuth2LoginSuccessHandler.java`** — обработчики успеха/ошибки входа.

### `model/` — JPA-сущности и enum'ы
- Базовые сущности: `Brand.java`, `Department.java`, `Role.java`, `StorageFile.java`, `Token.java`, `User.java`, `UserProvider.java`.
- `cart/`: `CartItem.java` — позиция корзины.
- `inventory/`: `Ingredient.java`, `Packaging.java`, `Stock.java`, `Supplier.java`, `Supply.java`, `SupplyItem.java`, `Unit.java`, `Warehouse.java`.
- `master/`: `MasterAccount.java`, `RoleMembership.java`, `UserMembership.java`.
- `order/`: `DeliveryMode.java`, `Order.java`, `OrderItem.java`, `OrderMessage.java`, `OrderReview.java`, `OrderStatus.java`.
- `pickup/`: `PickupPoint.java`.
- `product/`: `Product.java`, `ProductArchive.java`.
- `tags/`: `GroupTag.java`, `GroupTagArchive.java`.
- `userbrand/`: `DeliveryAddress.java`, `UserBrandMembership.java`.

### `notification/longpoll/`
- **`LongPollController.java`** — HTTP long-poll API.
- **`LongPollService.java`** — публикация/выдача событий long-poll.
- **`LongPollEvent.java`**, **`LongPollEventType.java`**, **`LongPollEnvelope.java`** — модель событий.
- **`LongPollAckRequest.java`** — ack запрос.
- **`LongPollExceptionAdvice.java`** — обработка ошибок long-poll.

### `repository/` — Spring Data JPA репозитории
- Базовые: `BrandRepository.java`, `DepartmentRepository.java`, `GroupTagRepository.java`, `GroupTagArchiveRepository.java`, `ProductRepository.java`, `ProductArchiveRepository.java`, `RoleRepository.java`, `StorageFileRepository.java`, `TokenRepository.java`, `UserRepository.java`, `UserProviderRepository.java`.
- `cart/`: `CartItemRepository.java`.
- `inventory/`: `IngredientRepository.java`, `PackagingRepository.java`, `StockRepository.java`, `SupplierRepository.java`, `SupplyRepository.java`, `SupplyItemRepository.java`, `UnitRepository.java`, `WarehouseRepository.java`.
- `master/`: `MasterAccountRepository.java`, `UserMembershipRepository.java`.
- `order/`: `OrderRepository.java`, `OrderItemRepository.java`, `OrderMessageRepository.java`, `OrderReviewRepository.java`.
- `pickup/`: `PickupPointRepository.java`.
- `userbrand/`: `DeliveryAddressRepository.java`, `UserBrandMembershipRepository.java`.

### `security/`
- **`CustomUserDetails.java`** — адаптер пользователя под Spring Security.
- **`JwtAuthenticationFilter.java`** — фильтр аутентификации по JWT.
- **`JwtAuthenticator.java`** — валидация/аутентификация токенов.
- **`JwtTokenExtractor.java`** — извлечение токена из заголовков/куков.
- **`JwtUtils.java`** — генерация/парсинг JWT.
- **`RbacGuard.java`** — проверки доступа по ролям/контексту.
- **`SecurityConfig.java`** — цепочка фильтров, правила доступа, CORS, CSRF и т.п.

### `service/`
- Базовые сервисы:
  - **`AuthService.java`** — логин/регистрация/refresh/связка провайдеров.
  - **`BotService.java`** — операции бота.
  - **`BrandService.java`** — бизнес-логика брендов.
  - **`CartService.java`** — интерфейс корзины.
  - **`CheckoutService.java`** — интерфейс чекаута.
  - **`CookieService.java`** — работа с куками (HttpOnly, домены, удаление).
  - **`GroupTagService.java`** — управление групп-тегами и деревом.
  - **`ImageProcessingService.java`** — обработка изображений (resize/форматы/WebP).
  - **`IngredientService.java`** — логика для ингредиентов.
  - **`MailService.java`** — отправка email.
  - **`MasterAccountService.java`** — мастер-аккаунты/инициализация.
  - **`MediaService.java`** — хранение/линки медиа, метаданные.
  - **`MembershipService.java`** — интерфейс работы с членством/ролями.
  - **`PackagingService.java`** — логика упаковки.
  - **`ProductService.java`** — логика продуктов, архив, фильтры.
  - **`RoleService.java`** — роли и привязки.
  - **`S3StorageService.java`** — интеграция с S3 (Yandex Object Storage).
  - **`StaffService.java`** — управление сотрудниками/отделами.
  - **`StockService.java`** — остатки и движения.
  - **`SupplierService.java`** — поставщики.
  - **`SupplyService.java`** — поставки/позиции поставок.
  - **`TokenService.java`** — refresh-токены, ревокация.
  - **`UnitService.java`** — единицы измерения.
  - **`UserDetailsServiceImpl.java`** — загрузка пользователя для Spring Security.
  - **`UserProfileService.java`** — профиль пользователя, адреса.
  - **`UserProviderService.java`** — связка внешних провайдеров.
  - **`UserService.java`** — поиск/CRUD пользователей, спецификации.
  - **`UserSpecifications.java`** — JPA-спецификации по пользователям.
  - **`WarehouseService.java`** — склады.
- `admin/`: `ClientAdminService.java`, `OrderAdminService.java`, `impl/` — админ-операции и их реализации.
- `impl/`: `CartInterestScheduler.java` (планировщик интереса/уведомлений корзины), `CartServiceImpl.java`, `CheckoutServiceImpl.java`, `MembershipServiceImpl.java` — реализации интерфейсов.

### `tenant/` — многотенантность/контекст
- **`ContextAccess.java`** — доступ к текущему контексту (master/brand/location/membership).
- **`ContextEnforcementFilter.java`** — фильтр, навязывающий наличие контекста.
- **`ContextResolver.java`** — извлечение/построение контекста из JWT/заголовков/куков.
- **`CtxCookieFilter.java`** — фильтр управления контекстными куками.
- **`TenantContext.java`** — хранение контекста в ThreadLocal.
- **`TenantContextCleanupFilter.java`** — очистка контекста в конце запроса.

### `util/`
- **`Base64Utils.java`** — утилиты Base64.
- **`HmacSigner.java`** — подпись HMAC (интеграции/вебхуки).
- **`TextUtils.java`** — строковые утилиты.

## Backend: тесты `src/test/`
- `resources/application.yml` — тестовая конфигурация.
- `config/` — базовые классы интеграционных тестов:
  - `IntegrationTestBase.java` — общая база/инициализация контекста.
  - `TestEnvironment.java` — утилиты окружения/профили.
- `controller/` — тесты контроллеров (WebMvc/интеграционные):
  - Примеры: `AuthControllerTest.java`, `MediaControllerTest.java`, `ClientMessagingIntegrationTest.java`, `ContextSwitchIntegrationTest.java`, `GroupTagControllerRbacIT.java`, и подпакеты `cart/`, `checkout/`, `inventory/`, `order/`, `pickup/`, `profile/`, `publicapi/`.
- `googleOAuth2/` — тесты OAuth2: `GoogleOAuth2ServiceTest.java`, `OAuth2LoginSuccessHandlerTest.java`.
- `notification/longpoll/` — тесты лонгпола.
- `repository/` — изолированные тесты репозиториев + `order/`.
- `security/` — JWT/аутентификация/публичные ручки: `JwtAuthenticatorTest.java`, `JwtUtilsTest.java`, и т.д.
- `service/` — юниты/интеграционные сервисов: примеры `AuthServiceTest.java`, `ProductServiceIntegrationTest.java`, `GroupTagServiceIntegrationTest.java` и др.
- `tenant/` — `CtxCookieFilterTest.java`.
- `testutil/` — фикстуры/билдеры: `MembershipFixtures.java`, `TestOrderFactory.java`, и др.
- `util/` — `Base64UtilsTest.java`.

## Frontend `Front/`
- Корень фронта:
  - **`.env`** — переменные окружения фронтенда.
  - **`.gitignore`** — игнор-файл фронта.
  - **`Dockerfile`** — сборка и рантайм фронта (Nginx).
  - **`README.md`** — описание фронта.
  - **`index.html`** — HTML-шаблон Vite.
  - **`nginx.conf`** — конфиг Nginx для сервинга фронта/прокси к API.
  - **`package.json`**, **`package-lock.json`** — зависимости/скрипты.
  - **`public/`** — статические файлы (favicon, sitemap, vite.svg).
  - **`src/`** — исходники Vue 3.
  - **`tests/`** — модульные/E2E smoke тесты, фикстуры.
  - **`vite.config.js`** — конфиг Vite (алиасы, прокси).
- `src/` ключевые файлы:
  - **`main.js`** — iнициализация приложения Vue (router, store, плагины).
  - **`App.vue`** — корневой компонент.
  - **`admin.css`**, **`style.css`**, **`theme.css`** — стили.
  - **`router/index.js`** — маршруты, гарды, защита по ролям/контексту.
  - **`store/*.js`** — Pinia-сторы: `auth`, `cart`, `product`, `tag`, `notifications`, `inventoryStore`, `brand`, `order`, `ui`.
  - **`services/*.js`** — API-клиенты: `api.js` (интерцепторы, токены, контекст), `authService.js`, `productService.js`, `tagService.js`, `notifications.js`, `order*Service.js`, `inventory/*`, и др.
  - **`components/`** — UI-компоненты и модалки: `AppHeader.vue`, `Sidebar.vue`, `ProductCard.vue`, `Modal.vue`, `CheckoutModal.vue`, `Create/Edit*Modal.vue`, `ProfileEditModal.vue`, `TagManager.vue`, `UserForm.vue`, инвентарные формы и т.п.
  - **`views/`** — страницы: `AdminView.vue`, `HomeView.vue`, `LoginView.vue`, `RegisterView.vue`, `ProductDetailView.vue`, `Profile*View.vue`, `CartView.vue`, `CheckoutView.vue`, `MyOrdersView.vue`, `StaffManagementView.vue`, `BrandsView.vue`, `TagsView.vue`, `inventory/*` и др.
  - **`utils/*.js`** — полезные функции: `jwt.js`, `datetime.js`, `brandHint.js`.
  - **`composables/`** — `useAcl.js`, `useDraggable.js`.
  - **`directives/can.js`** — директива доступа по ролям/контексту.
  - **`assets/`** — иконки, картинки, темы.
  - **`brand/default.css`** — базовая тема бренда.
  - **`theme/*.css`** — темы (светлая/тёмная).
- `tests/`
  - Юнит/интеграционные тесты: `*.spec.js` для компонентов/сторов/сервисов.
  - Фикстуры: `fixtures/*` (orders/products/staff/tags JSON).
  - Утилиты тестирования: `utils/aclTestUtils.js`, `setup.js`.

## Документация `docs/`
- `architecture/`: `er-schema.md` (ER-диаграммы), `multitenancy.md` (многотенантность).
- `backend/`: `acl-tests.md`, `cookies-and-cors.md`, `migrations.md`, `order-acl.md`, `swagger.md`, `tenant-context.md`, `tests.md`.
- `frontend/`: `acl.md`, `auth.md`, `context.md`, `frontindex.md`, `inventory-crud.md`, `inventory-stock.md`, `notifications-longpoll.md`.
- `services/`: `admin-clients.md`, `auth.md`, `context.md`, `errors.md`, `health.md`, `inventory.md`, `menu.md`, `rbac.md`, `rbac-access.md`, `supplies-and-stock.md`.
- **`index.md`** — навигатор по документации исходников.

## UpdateCert `updateCert/`
- **`docker-compose-create-cert.yml`** — окружение для генерации сертификатов (certbot/openssl и т.п.).
- **`renew-and-deploy.sh`** — скрипт продления и выката новых сертификатов (копирование в нужные пути, рестарт сервисов).

---

# Краткая карта ответственности (по слоям)
- **Контроллеры** — `controller/**`: REST API, валидация, делегирование в сервисы.
- **Сервисы** — `service/**`: бизнес-логика, работа с репозиториями и внешними сервисами (S3, Mail, OAuth2).
- **Репозитории** — `repository/**`: доступ к БД через Spring Data JPA.
- **Модели** — `model/**`: JPA-сущности и enum'ы.
- **Безопасность** — `security/**`: JWT, RBAC, конфигурация Security, фильтры.
- **Многотенантность** — `tenant/**`: контекст арендатора, фильтры и резолверы.
- **Исключения** — `exception/**`: доменные ошибки + глобальный хэндлер.
- **DTO** — `dto/**`: вход/выход объектов API.
- **Конфигурация** — `config/**`: CORS, Swagger, Web MVC, seed-инициализация.
- **Утилиты** — `util/**`: базовые служебные функции.
- **Миграции** — `resources/db/changelog/**`: Liquibase для схемы/данных.
- **Тесты** — `src/test/**`: покрытие контроллеров, сервисов, безопасности, репозиториев и т.д.
- **Фронтенд** — `Front/**`: Vue 3 приложение, роуты, сторы, сервисы API, компоненты и тесты.

Если нужно расширить отчёт примерами эндпоинтов/диаграммами связей — скажите, добавлю разделы со Swagger-линками и схемами.
