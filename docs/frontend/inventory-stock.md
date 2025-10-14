# Frontend: Страница остатков (Stock)

Документ описывает пользовательский интерфейс страницы остатков: фильтры, таблицу остатков, операции приход/списание, а
также маршрутизацию и роли доступа.

## Обзор

- Экран: `Front/src/views/inventory/StockView.vue`.
- Store: `Front/src/store/inventoryStore.js` (секции `stockRows`, `stockFilters` и экшены `fetchStock`, `increaseStock`,
  `decreaseStock`).
- Сервисы: `Front/src/services/inventory/stockService.js` (`listStock`, `increaseStock`, `decreaseStock`).
- Модальные окна: `Front/src/components/inventory/StockIncreaseModal.vue`,
  `Front/src/components/inventory/StockDecreaseModal.vue`.
- Маршрут: `/admin/inventory/stock` (см. `Front/src/router/index.js`).

## Доступ и роли

- Роут: `/admin/inventory/stock` задан с `meta.roles = ['ADMIN','OWNER','COOK','CASHIER']`.
- Операции приход/списание доступны только `ADMIN`/`OWNER` (защита через `v-can`), просмотр — всем указанным ролям.

## Фильтры

- Поля фильтра: `Склад (warehouseId)`, `Ингредиент (ingredientId)`.
- Кнопка «Показать» вызывает `store.fetchStock({ warehouseId, ingredientId })`.
- Источники данных для селектов подтягиваются из `store.fetchWarehouses()` и `store.fetchIngredients()` при
  инициализации.

## Таблица

Колонки, которые ожидает фронт от API:

- `ingredientId` — ID ингредиента
- `name` — имя ингредиента
- `unitName` — единица измерения (читаемое имя)
- `packageSize` — размер упаковки (если есть)
- `quantity` — агрегированный остаток
- `earliestExpiry` — ближайший срок годности
- `lastSupplyDate` — дата последней поставки

Источник данных: `store.stockRows` → маппится 1:1 в `StockView.vue`.

## Операции приход/списание

- Кнопки «Приход»/«Списание» доступны, когда выбран склад и (для строковых действий) известен `ingredientId`.
- Модалки:
    - `StockIncreaseModal.vue` — формирует payload `{ ingredientId, warehouseId, qty }` и вызывает
      `store.increaseStock(payload)`.
    - `StockDecreaseModal.vue` — то же самое, но вызывает `store.decreaseStock(payload)`; дополнительно на странице
      ограничение: списание не разрешено при `quantity <= 0`.
- После успешной операции стор автоматически вызывает `fetchStock(this.stockFilters)` для обновления таблицы.

## Маршрутизация

- Определение роута: `Front/src/router/index.js`,
  ```javascript
  import StockView from '../views/inventory/StockView.vue';

  export const routes = [
    {
      path: '/admin/inventory/stock',
      name: 'Stock',
      meta: { title: 'Остатки', requiresAuth: true, roles: ['ADMIN','OWNER','COOK','CASHIER'] },
      component: StockView,
    },
  ];
  ```
- Ссылка в боковом меню: `Front/src/components/Sidebar.vue` → раздел «Склад» → «Остатки» (виден при наличии нужных
  ролей).

## Взаимодействие с API

Используемые методы (см. `Front/src/services/inventory/stockService.js`):

- `GET /auth/v1/inventory/stock?warehouseId&ingredientId` — список остатков с агрегатами.
- `POST /auth/v1/inventory/stock/increase { ingredientId, warehouseId, qty }` — приход.
- `POST /auth/v1/inventory/stock/decrease { ingredientId, warehouseId, qty }` — списание.

Ожидаемые поля ответа списка — см. раздел «Таблица» выше.

## Acceptance-критерии

- **Фильтры** работают: при выборе склада/ингредиента и нажатии «Показать» таблица меняется.
- **Приход/Списание** корректно меняют таблицу; после успешной операции данные перезагружаются.
- **Доступ**: операциям приход/списание соответствуют роли; просмотр доступен указанным ролям.

## UX-детали

- В таблице «Списание» по строке отключено при `quantity <= 0`.
- Модалки используют списки ингредиентов/складов; для списания список ингредиентов ограничен теми, у которых qty > 0.

## Связанные документы

- Бэкенд-описание складов и ингредиентов: `docs/services/inventory.md`.
- Поставки и агрегаты остатков: `docs/services/supplies-and-stock.md`.
