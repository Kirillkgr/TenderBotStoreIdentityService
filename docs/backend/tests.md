# Тестовые фикстуры и сценарии

Этот документ описывает, как в интеграционных тестах быстро готовить роли, контексты и базовые сущности каталога.

## MembershipFixtures

Класс: `src/test/java/kirillzhdanov/identityservice/testutil/MembershipFixtures.java`

- `registerAndLogin(username)`: регистрирует пользователя и возвращает Cookie с `accessToken`.
- `login(username, password)`: логин существующего пользователя.
- `ensureLogin(username)`: кешируемый вход — пытается взять токен из кеша, далее `login`, иначе `registerAndLogin`.
- `switchContext(cookie, membershipId)`: меняет JWT‑контекст на указанный membership.
- `prepareAllRoleMemberships(cookie, username)`: создаёт по membership на роли `OWNER, ADMIN, CASHIER, COOK, CLIENT` (по
  одному Master на роль) и возвращает карту `role -> Context`.
- `prepareRoleMembership(cookie, username, role)`: создаёт один membership и возвращает `Context`.
- `prepareRoleMembershipInMaster(cookie, username, role, master)`: то же, но в заданном `MasterAccount`.

`Context` содержит: `membershipId`, `masterId`, `cookie` (после `context/switch`).

## ScenarioBuilder

Класс: `src/test/java/kirillzhdanov/identityservice/testutil/ScenarioBuilder.java`

- `createBrand(cookie, masterId, name, orgName) -> long`: создаёт бренд через `/auth/v1/brands` (требуются права
  OWNER/ADMIN и заголовок `X-Master-Id`).
- `createProduct(cookie, name, price, brandId) -> long`: создаёт товар через `/auth/v1/products` (требуются права
  OWNER/ADMIN).

## Примеры использования

```java
@Autowired MembershipFixtures fx;
@Autowired ScenarioBuilder sb;

Cookie login = fx.ensureLogin("test-user");
var admin = fx.prepareRoleMembership(login, "test-user", RoleMembership.ADMIN);
long brandId = sb.createBrand(admin.cookie(), admin.masterId(), "Brand-A", "Org-A");
long productId = sb.createProduct(admin.cookie(), "Prod-1", new BigDecimal("10.00"), brandId);
```

## Тесты

Проверка работоспособности фикстур: `src/test/java/kirillzhdanov/identityservice/testutil/MembershipFixturesTest.java`.

## Замечания

- В интеграционных тестах действует `ContextEnforcementFilter`: для защищённых ручек нужен tenant‑контекст (`masterId` в
  dev через `X-Master-Id` или `context/switch`).
- Для админ‑операций используйте роли `OWNER/ADMIN` (см. `RbacGuard`).
- `ensureLogin` кеширует `accessToken` на время JVM — ускоряет многократные тест-кейсы с одним пользователем.
