# ACL: матрица и рекомендации по бэкенд‑тестам

Этот документ описывает, какие роли имеют доступ к ключевым эндпоинтам,какие статусы ожидаются (
200/201/204/403/404/409), и как писать интеграционные тесты для проверки ACL.

## Роли

- Глобальные: OWNER, ADMIN, USER
- Контекстные: CASHIER, COOK, CLIENT

## Маршруты и ожидаемые статусы

- Заказы (admin)
    - GET /admin/orders
        - OWNER/ADMIN/CASHIER → 200
        - USER/CLIENT/COOK/гость → 403
    - PATCH /admin/orders/{id}/status
        - OWNER/ADMIN/CASHIER → 200
        - USER/CLIENT/COOK/гость → 403
        - Невалидный переход статуса → 409 (для разрешённых ролей)
        - Не найден заказ → 404

- Персонал (staff)
    - POST /admin/users
        - OWNER/ADMIN → 201
        - Иные роли → 403
    - PUT /admin/users/{id}
        - OWNER/ADMIN → 200
        - Иные роли → 403
    - DELETE /admin/users/{id}
        - OWNER/ADMIN → 204
        - Иные роли → 403

- Каталог (админские CRUD)
    - POST/PUT/DELETE /admin/products
    - POST/PUT/DELETE /auth/v1/group-tags
    - POST/PUT/DELETE /admin/brands (если есть)
        - OWNER/ADMIN → 2xx
        - Иные роли → 403

- Кухня (kitchen)
    - Доступ к эндпоинтам кухни
        - COOK/OWNER/ADMIN → 200
        - Иные роли → 403

- Переключение контекста
    - POST /auth/v1/context/switch
        - Валидный membership → 200, новый accessToken с требуемой ролью
        - Невалидный membership → 403/404

## Рекомендуемые интеграционные тесты

Размещать под `src/test/java/.../controller/` (или актуальный пакет):

- OrderStatusAclIntegrationTest
    - PATCH /admin/orders/{id}/status
        - `@WithMockUser(roles = {"OWNER"})` → 200
        - `@WithMockUser(roles = {"ADMIN"})` → 200
        - `@WithMockUser(roles = {"CASHIER"})` → 200
        - `@WithMockUser(roles = {"USER"})` → 403
        - `@WithMockUser(roles = {"COOK"})` → 403
        - Невалидный переход → 409
        - Не найден заказ → 404

- OrdersAccessAclIntegrationTest
    - GET /admin/orders
        - OWNER/ADMIN/CASHIER → 200
        - Остальные → 403

- StaffAclIntegrationTest
    - POST/PUT/DELETE /admin/users → только OWNER/ADMIN → 2xx; иные → 403

- CatalogAdminAclIntegrationTest
    - POST/PUT/DELETE /admin/products, /auth/v1/group-tags, /admin/brands → только OWNER/ADMIN → 2xx; иные → 403

- ContextSwitchIntegrationTest (расширение)
    - Валидный membership даёт 200 и новый accessToken с ожидаемым набором ролей
    - Невалидный membership → 403/404

## Примеры тестов (шаблоны)

```java
// Пример: смена статуса доступна кассиру
@SpringBootTest
@AutoConfigureMockMvc
class OrderStatusAclIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "u", roles = {"CASHIER"})
    void cashier_can_update_status() throws Exception {
        mvc.perform(patch("/admin/orders/{id}/status", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PREPARING\"}"))
           .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "u", roles = {"USER"})
    void user_forbidden_update_status() throws Exception {
        mvc.perform(patch("/admin/orders/{id}/status", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PREPARING\"}"))
           .andExpect(status().isForbidden());
    }
}
```

```java
// Пример: создание пользователя доступно только ADMIN/OWNER
@SpringBootTest
@AutoConfigureMockMvc
class StaffAclIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void admin_can_create_user() throws Exception {
        mvc.perform(post("/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"u1\",\"roles\":[\"USER\"]}"))
           .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void user_forbidden_create_user() throws Exception {
        mvc.perform(post("/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"u1\",\"roles\":[\"USER\"]}"))
           .andExpect(status().isForbidden());
    }
}
```

## Советы по реализации

- Нормализация ролей: backend обычно ожидает значения без префикса `ROLE_` в `@WithMockUser(roles = {...})`. Убедитесь в
  единообразии.
- Мульти‑тенантность: добавляйте проверки изоляции (например, доступ к заказам только в рамках мастера/бренда).
- Конфликты статусов: держите карту допустимых переходов и проверяйте 409 для запрещённых.
- Контекст‑переключение: проверяйте клеймы нового токена (если декодируете JWT в тестах) или косвенно — доступ к
  защищённому эндпоинту после switch.

## Запуск тестов

- Maven: `mvn -q -Dtest=*Acl* test` (или общий `mvn test`)
- Отчёты: смотрите `target/surefire-reports/`

## Связь с фронтендом

Фронтенд smoke‑тесты (Vitest) проверяют видимость/доступность элементов (директива `v-can`) и корректность
автопереключения контекста в `AppHeader.vue`. Бэкенд‑тесты закрепляют тот же контракт на уровне HTTP, чтобы
минимизировать регрессии при изменениях прав.
