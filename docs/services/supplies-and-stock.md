# Supplies and Stock (Поставки и остатки)

Простой, но подробный гайд по движению товара на складе: от создания поставки до обновления агрегированных остатков (
`stock`).

### Коротко

Supplies-and-Stock: создание поставки (DRAFT), проведение (POSTED), обновление stock с агрегатами:

- `earliestExpiry = min(expiresAt)` среди партий
- `lastSupplyDate = supply.date`

## Сущности

- **Supply** — поставка: `masterId`, `warehouseId`, `date`, `notes`, `status` (`DRAFT`|`POSTED`).
- **SupplyItem** — позиция поставки: `ingredientId`, `qty`, `expiresAt?`.
- **Stock** — агрегированный остаток по `(master, warehouse, ingredient)` с полями:
    - `quantity` — текущее количество
    - `earliestExpiry` — минимальный срок годности среди партий
    - `lastSupplyDate` — дата последней проведённой поставки

См. ER: Wiki → ER-Schema (Inventory) или `docs/architecture/er-schema.md`.

## Порядок работы (создание и проведение поставки)

1. Создать поставку (DRAFT):
    - `POST /auth/v1/inventory/supplies`
    - Пример:
   ```json
   {
     "warehouseId": 5,
     "date": "2025-10-11T10:00:00Z",
     "notes": "поставка от поставщика X",
     "items": [
       { "ingredientId": 101, "qty": 2.5, "expiresAt": "2025-12-31" },
       { "ingredientId": 102, "qty": 1.0 }
     ]
   }
   ```
    - Валидации: `items` не пустой, `qty > 0`, корректные `ingredientId/warehouseId`.

2. Провести поставку (POSTED):
    - `POST /auth/v1/inventory/supplies/{id}/post`
    - Эффект:
        - Увеличить `stock.quantity` по каждой позиции (`warehouseId`, `ingredientId`).
        - Обновить агрегаты:
            - `earliestExpiry` = минимум по `expiresAt` среди партий (если было задано)
            - `lastSupplyDate` = `supply.date`

3. Проверить остатки склада:
    - `GET /auth/v1/inventory/stock?warehouseId=5`
    - Ответ содержит строки вида `{ ingredientId, name, quantity, earliestExpiry, lastSupplyDate }`.

## Постановка ингредиентов на склад (Variant A из формы ингредиента)

При создании ингредиента фронтом можно сразу задать начальное количество и склад.

- Запрос к `POST /auth/v1/inventory/ingredients` с полями:
  ```json
  {
    "name": "Сахар",
    "unitId": 10,
    "warehouseId": 5,
    "initialQty": 2.5,
    "notes": null
  }
  ```
- Бэкэнд создаёт поставку DRAFT и тут же проводит её (POSTED):
    - Созданная партия попадает в `stock`.
    - Если `initialQty` не задан или `0` — ingredient создаётся без движения по складу.

## RBAC и контекст

- Для всех `/auth/v1/**` требуется активный контекст (см. Wiki → Context).
- OWNER/ADMIN — могут создавать/проводить поставки и менять остатки.
- COOK/CASHIER — только чтение (склады, остатки, ингредиенты).

## Частые ошибки

- 400 Bad Request: `qty` <= 0, пустой `items`, неизвестный `ingredientId/warehouseId`.
- 403 Forbidden: нет роли или контекста (проверьте заголовки/JWT, см. Context).
- 404 Not Found: попытка работать с чужими сущностями (другой master) маскируется как не найдено.
