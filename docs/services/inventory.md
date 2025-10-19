# Inventory (Склады и ингредиенты)

Краткое руководство по работе со складским учётом на стороне Identity Service. Раздел описывает сущности и базовые
операции для фронта BL3-09.

### Коротко

Inventory: как создать склад/единицу/ингредиент и в чём роль фасовок. При создании ингредиента можно сразу поставить на
склад (Variant A), что создаст и проведёт поставку.

## Модель (сущности)

- **Warehouse** — склад, привязан к `master`.
- **Unit** — единица измерения (например, кг, л, шт), общая для ингредиентов и фасовок.
- **Ingredient** — ингредиент с базовой единицей измерения и опциональной фасовкой/размером упаковки.
- **Packaging** — тип фасовки (имя, единица, размер), уникален в рамках `master`.
- См. ER: [Wiki → ER‑схема](wiki/er-schema) (блок Inventory) или `docs/architecture/er-schema.md`.

Связи (упрощённо):
- `Ingredient (1) — (N) StockRow` через движение остатков по складам.
- `Packaging` ссылается на `Unit` (размер фасовки в единицах измерения).
- `Supply` (поставка) создаёт движения по `Stock`.

## Эндпоинты (основные)

- `GET /auth/v1/inventory/warehouses` — список складов.
- `POST /auth/v1/inventory/warehouses` — создать склад (OWNER/ADMIN).
- `GET /auth/v1/inventory/units` — список единиц.
- `POST /auth/v1/inventory/units` — создать единицу (OWNER/ADMIN).
- `GET /auth/v1/inventory/ingredients` — список ингредиентов.
- `POST /auth/v1/inventory/ingredients` — создать ингредиент (см. Variant A ниже).
- `GET /auth/v1/inventory/packagings` — список фасовок.
- `POST /auth/v1/inventory/packagings` — создать фасовку (OWNER/ADMIN).

Остатки и поставки:
- `GET /auth/v1/inventory/stock?ingredientId&warehouseId` — список остатков (фильтры).
- `POST /auth/v1/inventory/stock/increase` — приход (увеличить остаток).
- `POST /auth/v1/inventory/stock/decrease` — списание (уменьшить остаток).
- `POST /auth/v1/inventory/supplies` — создать поставку (DRAFT).
- `POST /auth/v1/inventory/supplies/search` — поиск/листинг поставок (POST для фильтров/пагинации).
- `GET /auth/v1/inventory/supplies/{id}` — получить поставку.
- `PUT /auth/v1/inventory/supplies/{id}` — обновить DRAFT.
- `POST /auth/v1/inventory/supplies/{id}/post` — провести поставку (POSTED).

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

## Статусы и DoD

Статусы поставки (`Supply.status`):
- `DRAFT` — черновик, редактируемый; не влияет на остатки до проведения.
- `POSTED` — проведена; движения по складу отражены, редактирование ограничено.

Definition of Done (DoD) для Inventory:
- CRUD по `Warehouses/Units/Ingredients/Packaging` с валидациями и кодами ошибок в Swagger (4xx/409).
- Поставки: создание DRAFT, обновление DRAFT, проведение в POSTED, просмотр, поиск с пагинацией.
- Остатки: API increase/decrease с проверкой достаточности и корректными ошибками.
- RBAC: OWNER/ADMIN — запись; COOK/CASHIER — чтение; гость — нет доступа.
- Документация: разделы Swagger группы `inventory`, страницы Wiki/Docs (настоящая страница), ссылки в индексах.

## RBAC‑матрица (вкратце)

| Роль            | Read (lists/get) | Create | Update | Delete | Stock inc/dec | Supplies |
|-----------------|------------------|--------|--------|--------|---------------|----------|
| OWNER / ADMIN   | да               | да     | да     | да     | да            | да       |
| COOK / CASHIER  | да               | нет    | нет    | нет    | нет           | просмотр |
| USER / CLIENT   | нет              | нет    | нет    | нет    | нет           | нет      |

Примечание: доступ ограничен текущим `master` (tenant‑контекст).

## Миграции

- Liquibase мастер: `src/main/resources/db/changelog/db.changelog-master.xml`.
- Изменения для инвентаря располагаются в датированных файлах под `src/main/resources/db/changelog/YYYY/MM/..-changelog.xml` и включаются через `<include/>`.
- См. руководство: [Migrations](wiki/migrations).

## Примечания по контексту

- Для всех `/auth/v1/**` требуется контекст мастера (см. `ContextEnforcementFilter`).
- В тестах/локально допускается `X-Master-Id` (dev‑fallback).
- Роли OWNER/ADMIN — запись; COOK/CASHIER — чтение.

См. также: Wiki → Context, Wiki → RBAC.
