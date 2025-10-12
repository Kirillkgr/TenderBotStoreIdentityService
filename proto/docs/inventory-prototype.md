# Inventory Page Prototype — Detailed Description

Этот документ описывает прототип страницы "Склад → Ингредиенты / Остатки", его данные и маппинг на API. Используйте
документ как руководство при разработке фронта и интеграции с backend (BL-3-INV).

## Цели

- Чётко описать поля таблицы и поведение элементов интерфейса.
- Дать рекомендации по API-запросам и структурам данных, которые нужны для реализации.
- Перечислить всех важных сущностей и их атрибуты для интеграции с бэкендом.

---

## Основные UI блоки

- Topbar (логотип, навигация, профиль, переключатель темы)
- Inventory header: поиск, селект склада, дата-фильтр, кнопки "Добавить ингредиент", "Приход/Списание"
- Warehouse tabs: список складов с возможностью переключения (active tab меняет цвет и фильтрует таблицу)
- Таблица ингредиентов: колонки описаны ниже
- Пагинация, per-page select
- Легенда состояний (Мало на складе, Просрочено)

## Таблица — колонки

- id (integer)
- name (string)
- currentQty (number) — текущее количество на выбранном складе
- unit (string)
- expiresAt (date)
- lastSupplyAt (date) — дата последней поставки (последнего прихода)
- lastUsedAt (date) — дата последнего использования/списания
- supplierName (string)
- category (string)
- actions (UI)

## Модальные окна

1) Создание склада (warehouse modal)

- Открывается по кнопке + справа от вкладок складов.
- Поля: name (string, required).
- Поведение: при успешном создании добавляется новая вкладка склада и select для выбора склада; новая вкладка
  автоматически выбирается.

2) Создание ингредиента (ingredient modal)

- Открывается по кнопке "+" рядом с вкладками (после выбора склада) либо по кнопке "Добавить ингредиент".
- Поля:
    - name (string, required)
    - unit (string, required) — единица измерения
    - category (string)
    - supplier (string)
    - initialQty (number, >=0)
    - expiresAt (date)
    - protein/fat/carbs (numbers, >=0)
    - notes (text)
- Поведение: создаёт запись ингредиента и добавляет её в таблицу для выбранного склада. Данные могут быть временно
  созданы на фронте (mock) или отправлены на backend через POST `/auth/v1/inventory/ingredients` с полем `warehouseKey`/
  `warehouseId`.

Валидации: name и unit обязательны; initialQty >= 0; protein/fat/carb >= 0.

В frontend-таблице строки помечены `data-warehouse` с ключом склада (напр. `central`, `kitchen2`). Это позволяет
фильтровать строки без запроса.

## Предлагаемый JSON-ответ от API (GET /auth/v1/inventory/ingredients?warehouseId=...)

{
"items": [
{
"id": 11,
"name": "Рис",
"currentQty": 12,
"unit": "кг",
"expiresAt": "2025-11-01",
"lastSupplyAt": "2025-09-23",
"lastUsedAt": "2025-10-02",
"supplier": {
"id": 7,
"name": "ООО \"АзияФуд\""
},
"category": "Крупы",
"warehouseKey": "central"
}
],
"page": 1,
"size": 10,
"total": 42
}

> Примечание: ключ `warehouseKey` используется на фронте для фильтрации вкладок; backend может вернуть `warehouseId`/
`warehouseKey`.

## Полезные эндпоинты (BL-3-INV)

- Units: GET/POST/PUT/DELETE `/auth/v1/inventory/units`
- Suppliers: GET/POST/PUT/DELETE `/auth/v1/inventory/suppliers`
- Warehouses: GET/POST/PUT/DELETE `/auth/v1/inventory/warehouses`
- Ingredients: GET/POST/PUT/DELETE `/auth/v1/inventory/ingredients` (поддержать query: warehouseId, page, size, search)
- Stock: GET `/auth/v1/inventory/stock?ingredientId=&warehouseId=`, POST `/auth/v1/inventory/stock/increase`, POST
  `/auth/v1/inventory/stock/decrease`

## Рекомендации по интеграции

- Опция 1 (Server-side filtering/paging): При выборе вкладки/склада делать вызов
  `GET /ingredients?warehouseId=...&page=...&size=...` → backend возвращает уже просуммированный `currentQty` и даты
  `lastSupplyAt`/`lastUsedAt`.
- Опция 2 (Client-side filtering): backend возвращает все данные, frontend фильтрует по `warehouseKey`. Подходит для
  небольших наборов данных.

## Поля и валидации (frontend)

- При создании/редактировании ингредиента: name required, unitId required, packageSize >= 0.
- Для операций stock: qty > 0; при decrease backend должен вернуть 400 если попытка уйти в минус.

## Сценарии UI

- Добавить ингредиент: открывается modal, после успешного создания таблица обновляется.
- Приход/Списание: modal с выбором склада, ингредиента и qty; после успеха — обновляем row (get single or refresh page).
- RBAC: кнопки create/edit/delete доступны только OWNER/ADMIN; COOK/CASHIER имеют доступ на чтение; CLIENT — deny.

## Привязка к BL-3-INV задачам

- Таблица использует endpoints BL3-08 (Ingredients) и BL3-10 (Stock).
- LastSupplyAt / LastUsedAt требуют логики на backend: хранить историю прихода/списания в отдельной сущности (
  transactions) и в ответе для списка отдавать агрегаты (последняя дата прихода/списания).

## Что нужно на backend для корректной работы страницы

1. В Ingredients API — поле `warehouseId`/`warehouseKey` + currentQty агрегированное по складу (или отдельный stock
   endpoint для получения qty и дат)
2. Endpoints Stock должны возвращать updated stock row и историю операций
3. RBAC должна быть доступна по токену, frontend должен получать role в user info

---

Документ можно расширять: добавить примеры ошибок API, схемы DTO (OpenAPI), и mapping SQL → DTO. Готов сгенерировать
OpenAPI-фрагменты или миграции/примерные DTO для backend — скажите, что нужно следующее.

---

## Полное соответствие прототипа и API — куда и как отправлять запросы

Ниже идёт подробная привязка каждой интерактивной точки прототипа к конкретному API-эндпоинту, с примерами тела запроса
и ответа, правилами RBAC и ожидаемым поведением фронта при ошибках.

Обозначения:

- UI элемент — где расположен (страница/модал/кнопка)
- Endpoint — HTTP путь и метод
- Body/Response — JSON пример
- RBAC — кто может вызывать
- Поведение фронта — что делать после успешного ответа и при ошибках

1) Создание единицы измерения (Units)

- UI элемент: страница "Единицы" → кнопка "Добавить" (modal UnitForm)
- Endpoint: POST `/auth/v1/inventory/units`
- Body:
  ```json
  { "name": "Килограмм", "shortName": "кг" }
  ```
- Response 201:
  ```json
  { "id": 1, "name": "Килограмм", "shortName": "кг" }
  ```
- RBAC: OWNER/ADMIN для мутаций; others read-only
- Frontend: при 201 закрыть modal, добавить в `units` store и обновить все selects (ingredient modal). При 400/409
  показать field-errors (inline). При 401/403 показать modal с правами/редирект на логин.

2) Создание поставщика (Suppliers)

- UI: страница "Поставщики" → "Добавить" modal
- Endpoint: POST `/auth/v1/inventory/suppliers`
- Body example:
  ```json
  { "name": "ООО Поставки", "phone": "+7...", "email": "p@ex.ru", "address": "ул..." }
  ```
- Response 201: created supplier object. RBAC: OWNER/ADMIN.
- Frontend: добавить в suppliers store, закрыть modal, обновить supplier selects.

3) Создание склада (Warehouses) — в прототипе «+» рядом с вкладками

- UI: '+' справа от вкладок; modal Create Warehouse
- Endpoint: POST `/auth/v1/inventory/warehouses`
- Body:
  ```json
  { "name": "Новый склад" }
  ```
- Response 201: `{ "id": 42, "name": "Новый склад" }`
- RBAC: OWNER/ADMIN
- Frontend:
    - На success: добавить вкладку, добавить option в select, выставить этот склад как active (в prototype реализовано).
    - На 409: показать inline-ошибку (склад с таким именем уже существует).

4) Создание ингредиента (Ingredients) — подробный поток

- UI: Inventory page → кнопка "Добавить ингредиент" или '+' (при выбранном складе) → ingredient modal
- Поля формы прототипа:
    - name (string, required)
    - unitId or unitShort (select, required)
    - packageSize (decimal, optional)
    - category (string)
    - supplierId (select) или supplierName
    - initialQty (decimal, optional) — количество для выбранного склада
    - expiresAt (date, optional)
    - protein/fat/carbs (numbers)
    - notes (text)

- Endpoint(s) и рекомендованная последовательность (надёжный, транзакционный подход):

  Вариант A — два шага (безопасно, разделяет создание справочника и приход):
    1) Создать ингредиент (справочник):
        - POST `/auth/v1/inventory/ingredients`
        - Body:
          ```json
          {
            "name": "Сыр (Пармезан)",
            "unitId": 1,
            "packageSize": 1.0,
            "category": "Сыры",
            "supplierId": 7,
            "notes": "Импорт",
            "protein": 25.0,
            "fat": 28.0,
            "carbs": 1.5
          }
          ```
        - Response 201: `{ "id": 123, ... }`

    2) Если `initialQty` > 0 — создать Supply (через документ приход) для выбранного склада:
        - POST `/auth/v1/inventory/supplies`
        - Body:
          ```json
          {
            "warehouseId": 42,
            "supplierId": 7,
            "date": "2025-10-11T12:00:00",
            "notes": "Initial stock",
            "items": [ { "ingredientId": 123, "qty": 10.0 } ]
          }
          ```
        - Response 201: supply object with id
        - POST `/auth/v1/inventory/supplies/{id}/post` — применить приход к stock в транзакции
        - Response 200: supply posted; frontend затем вызывает GET `/auth/v1/inventory/stock?warehouseId=42` чтобы
          обновить таблицу

  Вариант B — единый вызов (если backend поддерживает):
    - POST `/auth/v1/inventory/ingredients` с полем `initialStock: [ {"warehouseId":42, "qty": 10.0} ]`
    - Backend создает ingredient + stock в одной транзакции; Response 201 включает created ingredient and stock summary.

- RBAC: создание ингредиента — OWNER/ADMIN.
- Validation/errors: 400 (invalid fields), 409 (duplicate name for master), 404 (supplier/unit not found in master
  scope). Frontend: показывать field errors inline; при 409 — предложение переименовать.

5) Загрузка списка складов и units (инициализация страницы)

- UI: Inventory page load
- Calls (parallel):
    - GET `/auth/v1/inventory/warehouses` — populate tabs and select
    - GET `/auth/v1/inventory/units` — populate unit selects in ingredient modal
- Frontend: cache these lists in store; revalidate periodically or on mutation events.

6) Получение остатков для выбранного склада (таблица)

- Trigger: change active warehouse (tab/select)
- Endpoint: GET `/auth/v1/inventory/stock?warehouseId={id}&q={optional}`
- Response (recommended fields):
  ```json
  [
    {
      "ingredientId": 123,
      "ingredientName": "Сыр (Пармезан)",
      "unitId": 1,
      "unitName": "кг",
      "qty": 2.0,
      "lastSupplyAt": "2024-08-20T10:00:00",
      "lastUsedAt": "2024-09-15T15:00:00",
      "expiresAt": "2024-09-01",
      "supplierName": "ООО \"ЕвроФуд\"",
      "category": "Сыры"
    }
  ]
  ```
- Frontend: render rows, highlight low/expired states. For big datasets use server paging.

7) Быстрое изменение остатков (Adjust stock)

- UI: inline +/- buttons or Stock modal (приход/списание)
- Two approaches:
    - Direct adjust endpoint (simple admin action):
      POST `/auth/v1/inventory/stock` (or `/stock/adjust`) body `{ warehouseId, ingredientId, qty }` — upsert/add qty (
      can be positive or negative if allowed)
      Response 200: `{ ingredientId, warehouseId, qty: newQty }`
    - Document-based (recommended): create Supply for increase, create Write-off/Consumption doc for decrease — then
      POST `/supplies/{id}/post` to apply
- RBAC: adjustments — OWNER/ADMIN
- Frontend: on success refresh stock for current warehouse; on 400 show appropriate message (e.g., cannot decrease below
  zero)

8) Создание/публикация поставки (Supplies)

- UI: Supplies page or modal from Inventory page
- Endpoint sequence:
    - POST `/auth/v1/inventory/supplies` — create draft supply
    - PUT `/auth/v1/inventory/supplies/{id}` — edit draft
    - POST `/auth/v1/inventory/supplies/{id}/post` — post/apply to stock
    - POST `/auth/v1/inventory/supplies/{id}/cancel` — cancel
- Validation: items[].ingredientId must exist; quantities positive; warehouse in scope; supplier in scope
- Frontend: after posting, refresh stock and supply list; keep supply history for audit.

9) Удаление / правки (delete/update)

- Delete warehouse: DELETE `/auth/v1/inventory/warehouses/{id}` — backend must prevent deletion when referenced by
  critical data (or implement soft-delete)
- Delete ingredient: DELETE `/auth/v1/inventory/ingredients/{id}` — check stock and supply history

10) Errors & UX rules

- Always show server `message` field on toasts for 4xx/5xx; highlight field-level errors in forms.
- For `409 Conflict` offer inline resolving options (rename/select existing).
- For long running transactions (posting supplies) show progress UI and disable repeated submits.

## Где должно происходить создание каждой сущности (связь с UI)

- Единица измерения (Unit): создаётся в `Units` page (modal) — POST `/units`. Ingredient modal использует units list.
- Поставщик (Supplier): создаётся в `Suppliers` page — POST `/suppliers`.
- Склад (Warehouse): создаётся в Inventory -> '+' рядом с вкладками — POST `/warehouses`.
- Ингредиент (Ingredient): создаётся в Ingredient modal. Backend: POST `/ingredients` (справочник). Если требуется
  начальный приход — создать Supply и post it, либо использовать backend-сокращение, если поддерживается.
- Поставка (Supply): создаётся в Supplies UI или как part of ingredient initial stock flow — POST `/supplies` then
  `/supplies/{id}/post`.
- Операция по остаткам (Quick stock adjust): можно реализовать напрямую через `/stock` API для быстрых правок, но
  рекомендуется работать через документную модель Supplies/Write-offs для аудита.

---

Если нужно, я могу:

- сгенерировать OpenAPI-фрагменты для перечисленных endpoint-ов (YAML или JSON), включая схемы request/response и коды
  ошибок;
- подготовить примеры SQL-моделей (DDL) и JPA-entity для backend (включая unique 제한, FK и индексы);
- реализовать mock-server (MSW) и адаптеры на фронте, чтобы текущий прототип работал над mock-API и затем легко
  переключался на реальный backend.

Скажите, что предпочитаете: добавить OpenAPI или mock-server в репозиторий первым шагом?