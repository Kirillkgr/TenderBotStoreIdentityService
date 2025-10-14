# Инвентарь (API справочник для прототипа страницы «Остатки»)

Этот файл агрегирует все ручки по складскому учёту для фронтенд‑дизайна и прототипирования. Основа: аутентификация
обязательна (tenant в JWT). Чтение доступно любому аутентифицированному пользователю (в т.ч. COOK/CASHIER). Мутации —
только OWNER/ADMIN.

## Общие замечания

- **Base path**: `/auth/v1/inventory`
- **RBAC**:
  - Чтение: AUTH (OWNER/ADMIN/COOK/CASHIER)
  - Мутации: OWNER/ADMIN
- **Типовые ошибки**: 400 (валидация/бизнес‑правила), 401 (нет аутентификации), 403 (нет прав), 404 (не найдено/другая
  аренда)
- **Даты**: в ответах даты в ISO; отображать как YYYY‑MM‑DD

---

## Units (Единицы измерения)

- GET `/auth/v1/inventory/units`
  - Описание: список units активного master
  - RBAC: AUTH
  - Ответ: `UnitDto[]`
  - Пример:
    ```bash
    curl -H "Authorization: Bearer <token>" \
      http://localhost:8081/auth/v1/inventory/units
    ```

- POST `/auth/v1/inventory/units`
  - Описание: создать unit
  - RBAC: OWNER/ADMIN
  - Тело: `UnitDto { name, shortName? }`
  - Ответ: 201 `UnitDto`

- PUT `/auth/v1/inventory/units/{id}` → 200 `UnitDto`
- DELETE `/auth/v1/inventory/units/{id}` → 204

---

## Warehouses (Склады)

- GET `/auth/v1/inventory/warehouses`
  - Описание: список складов master
  - RBAC: AUTH
  - Ответ: `WarehouseDto[]`

- POST `/auth/v1/inventory/warehouses`
  - RBAC: OWNER/ADMIN
  - Тело: `CreateWarehouseRequest { name }`
  - Ответ: 201 `WarehouseDto`

- PUT `/auth/v1/inventory/warehouses/{id}`
  - RBAC: OWNER/ADMIN
  - Тело: `UpdateWarehouseRequest { name }`
  - Ответ: 200 `WarehouseDto`

- DELETE `/auth/v1/inventory/warehouses/{id}` → 204

---

## Ingredients (Ингредиенты)

- GET `/auth/v1/inventory/ingredients`
  - Описание: список ингредиентов
  - RBAC: AUTH
  - Ответ: `IngredientDto[]`

- POST `/auth/v1/inventory/ingredients`
  - RBAC: OWNER/ADMIN
  - Тело: `CreateIngredientRequest { name, unitId, packageSize?, notes?, warehouseId?, initialQty? }`
  - Особенности:
    - Если заданы `warehouseId` и `initialQty > 0` — создаётся и проводится поставка (Variant A), что увеличивает
      `stock`
  - Ответ: 201 `IngredientDto`

- PUT `/auth/v1/inventory/ingredients/{id}` → 200 `IngredientDto`
- DELETE `/auth/v1/inventory/ingredients/{id}` → 204

---

## Packagings (Фасовки)

- GET `/auth/v1/inventory/packagings` → `PackagingDto[]` (RBAC: AUTH)
- POST `/auth/v1/inventory/packagings` (RBAC: OWNER/ADMIN)
  - Тело: `PackagingDto { name, unitId, size, ... }`
  - Ответ: 201 `PackagingDto`
- PUT `/auth/v1/inventory/packagings/{id}` → 200 `PackagingDto`
- DELETE `/auth/v1/inventory/packagings/{id}` → 204

---

## Suppliers (Поставщики)

- GET `/auth/v1/inventory/suppliers` → `SupplierDto[]` (RBAC: AUTH)
- POST `/auth/v1/inventory/suppliers` (RBAC: OWNER/ADMIN)
  - Тело: `SupplierDto { name, contacts?... }`
  - Ответ: 201 `SupplierDto`
- PUT `/auth/v1/inventory/suppliers/{id}` → 200 `SupplierDto`
- DELETE `/auth/v1/inventory/suppliers/{id}` → 204

---

## Supplies (Поставки)

- POST `/auth/v1/inventory/supplies`
  - Описание: создать поставку (DRAFT)
  - RBAC: OWNER/ADMIN
  - Тело: `CreateSupplyRequest`
    ```json
    {
      "warehouseId": 5,
      "date": "2025-10-11T10:00:00Z",
      "notes": "поставка X",
      "items": [
        { "ingredientId": 101, "qty": 2.5, "expiresAt": "2025-12-31" },
        { "ingredientId": 102, "qty": 1.0 }
      ]
    }
    ```
  - Ответ: 201 `{ id, status: "DRAFT" }`

- POST `/auth/v1/inventory/supplies/{id}/post`
  - Описание: провести поставку (POSTED)
  - Эффект: увеличить `stock.quantity` по позициям, обновить агрегаты:
    - `earliestExpiry = min(expiresAt)` среди партий
    - `lastSupplyDate = supply.date`
  - RBAC: OWNER/ADMIN
  - Ответ: 200 `{ id, status: "POSTED" }`

---

## Stock (Остатки)

- GET `/auth/v1/inventory/stock?ingredientId?&warehouseId?`
  - Описание: список строк остатков по фильтрам (нужен хотя бы один параметр)
  - Параметры:
    - `warehouseId: Long`
    - `ingredientId: Long`
  - RBAC: AUTH
  - Ответ: `StockRowDto[]`:
    ```json
    [
      {
        "ingredientId": 101,
        "name": "Сахар",
        "unitId": 10,
        "unitName": "кг",
        "packageSize": 1.0,
        "quantity": 2.5,
        "earliestExpiry": "2025-12-31",
        "lastSupplyDate": "2025-10-11",
        "lastUseDate": null,
        "supplierName": null,
        "categoryName": null
      }
    ]
    ```
  - Ошибки: 400 если не переданы оба фильтра

- POST `/auth/v1/inventory/stock/increase`
  - Описание: ручной приход (upsert, если записи не было)
  - RBAC: OWNER/ADMIN
  - Тело: `StockAdjustRequest { ingredientId, warehouseId, qty>=0 }`
  - Ответ: 200 `StockRowDto`
  - Ошибки: 400 (qty < 0 / неизвестные сущности), 403, 401
  - Пример:
    ```bash
    curl -X POST \
      -H "Authorization: Bearer <token>" \
      -H "Content-Type: application/json" \
      -d '{"ingredientId":101, "warehouseId":5, "qty":2.5}' \
      http://localhost:8081/auth/v1/inventory/stock/increase
    ```

- POST `/auth/v1/inventory/stock/decrease`
  - Описание: ручное списание (запрет на отрицательный остаток)
  - RBAC: OWNER/ADMIN
  - Тело: `StockAdjustRequest`
  - Ответ: 200 `StockRowDto`
  - Ошибки: 400 при попытке уйти в минус, 403, 401
  - Пример:
    ```bash
    curl -X POST \
      -H "Authorization: Bearer <token)" \
      -H "Content-Type: application/json" \
      -d '{"ingredientId":101, "warehouseId":5, "qty":1.0}' \
      http://localhost:8081/auth/v1/inventory/stock/decrease
    ```

---

## UX подсказки для страницы «Остатки»

- **Фильтры**: селекты склада и ингредиента. Не слать запрос без фильтров (бэкенд вернёт 400)
- **Таблица**: `name`, `unitName`, `packageSize`, `quantity`, `earliestExpiry`, `lastSupplyDate`
- **Кнопки «Приход»/«Списание»**: показывать только OWNER/ADMIN
- **После успешного POST**: закрывать модалку и обновлять список текущими фильтрами
- **Ошибки**: показывать текст 400/403; при 401 — стандартный флоу авторизации
