# Inventory (Склады и ингредиенты)

Краткое руководство по работе со складским учётом на стороне Identity Service. Раздел описывает сущности и базовые
операции для фронта BL3-09.

### Коротко

Inventory: как создать склад/единицу/ингредиент и в чём роль фасовок. При создании ингредиента можно сразу поставить на
склад (Variant A), что создаст и проведёт поставку.

## Сущности

- **Warehouse** — склад, привязан к `master`.
- **Unit** — единица измерения (например, кг, л, шт), общая для ингредиентов и фасовок.
- **Ingredient** — ингредиент с базовой единицей измерения и опциональной фасовкой/размером упаковки.
- **Packaging** — тип фасовки (имя, единица, размер), уникален в рамках `master`.
- См. ER: [Wiki → ER‑схема](wiki/er-schema) (блок Inventory) или `docs/architecture/er-schema.md`.

## Эндпоинты (основные)

- `GET /auth/v1/inventory/warehouses` — список складов.
- `POST /auth/v1/inventory/warehouses` — создать склад (OWNER/ADMIN).
- `GET /auth/v1/inventory/units` — список единиц.
- `POST /auth/v1/inventory/units` — создать единицу (OWNER/ADMIN).
- `GET /auth/v1/inventory/ingredients` — список ингредиентов.
- `POST /auth/v1/inventory/ingredients` — создать ингредиент (см. Variant A ниже).
- `GET /auth/v1/inventory/packagings` — список фасовок.
- `POST /auth/v1/inventory/packagings` — создать фасовку (OWNER/ADMIN).

RBAC: мутирующие операции доступны ролям OWNER/ADMIN; чтение — согласно политике (см. Wiki → RBAC).

## Создание ингредиента: Variant A (с постановкой на склад)

Запрос:

```json
{
  "name": "Сахар",
  "unitId": 10,
  "warehouseId": 5,
  "initialQty": 2.5,
  "packagingId": null,
  "packageSize": null,
  "notes": "опционально"
}
```

Правила:

- Если указаны `warehouseId` и `initialQty > 0`, сервис создаёт поставку в статусе DRAFT и сразу проводит её (POSTED).
- В результате `stock` по `(warehouseId, ingredientId)` увеличивается на `initialQty`.
- Если `initialQty` не задан или равен 0 — создаётся только ингредиент (без движения по складу).

Ответ: `IngredientDto` с `id` для дальнейших операций.

## Фасовки (Packaging)

- Хранятся глобально в рамках `master`.
- Используются при создании поставок (расчёт итогового количества по формуле `size × packageCount`).
- Валидации: `name` (required), `unitId` (required), `size >= 0`.

## RBAC и контекст

- Для всех `/auth/v1/**` требуется контекст мастера.
- В тестах/локально можно использовать заголовок `X-Master-Id` (dev).
- Роли OWNER/ADMIN — полные права на инвентарь; COOK/CASHIER — чтение.

См. также: Wiki → Context, Wiki → RBAC.
