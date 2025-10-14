# Админ: Клиенты (мастер-область, бренд и дата последнего заказа)

Документ описывает, как админ/владелец видит клиентов в системе: только тех, у кого есть заказы у текущего мастера, а
также какие дополнительные поля возвращаются.

## Коротко

- Клиент показывается, если выполнены два факта:
    1) у пользователя есть заказ,
    2) этот заказ принадлежит текущему мастеру (области видимости пользователя).
- Возвращаются поля бренда и даты последнего заказа по этому мастеру.
- Реализовано одним SQL-запросом на стороне БД (без N+1).

## Эндпоинт

- `GET /admin/v1/clients` — возвращает страницу `Page<ClientDto>`.
    - Параметры:
        - `page`/`size` — пагинация.
        - `search` — поиск по логину/ФИО/email/телефону (необязательно).
    - Роли: `ADMIN`, `OWNER`.

Пример ответа элемента `content`:

```json
{
  "id": 2,
  "firstName": "Иван",
  "lastName": "Иванов",
  "patronymic": "Иванович",
  "email": "ivan@example.com",
  "phone": "+79990000000",
  "dateOfBirth": "1990-01-10",
  "lastOrderAt": "2025-10-14T20:16:52.664084",
  "lastOrderBrandId": 3,
  "lastOrderBrand": "Brand X"
}
```

## Поля DTO

`ClientDto`:

- `id`, `firstName`, `lastName`, `patronymic`, `email`, `phone`
- `dateOfBirth: LocalDate`
- `lastOrderAt: LocalDateTime` — время последнего заказа в рамках текущего мастера
- `lastOrderBrandId: Long` — ID бренда последнего заказа
- `lastOrderBrand: String` — имя бренда последнего заказа

Удалены (устарели): `masterId`, `masterName` (больше не возвращаются на фронт).

## Область видимости (master)

- `effectiveMasterId` определяется из контекста текущего пользователя:
    - Для `ADMIN`/`OWNER`: `user.masterId`. Если пуст — пробуем определить через привязанные бренды пользователя (
      `brand.master.id`).
    - Без определённого master — пустая страница (данные не раскрываются).
- Отбор клиентов: только те пользователи, для которых существует заказ `Order` с `o.master_id = :effectiveMasterId`.

## Поиск

- Параметр `search` ищет по полям:
    - `username`, `firstName`, `lastName`, `patronymic`, `email`, `phone` (регистр не учитывается).

## Производительность

- Репозиторий `OrderRepository.findClientsByMasterWithLastOrder(masterId, search, pageable)`
    - Нативный SQL с CTE и `distinct on (client_id)` выбирает для каждого клиента последний заказ в рамках мастера.
    - Возвращает проекцию `ClientProjection` с агрегированными полями (время и бренд последнего заказа).
- Рекомендованные индексы:
    -
    `create index if not exists idx_orders_master_client_created_desc on orders (master_id, client_id, created_at desc);`
    - Для поиска в `users` — `pg_trgm` GIN индексы на `username/first_name/last_name/patronymic/email/phone`.

## Фронтенд

- Экран: `Front/src/views/AdminClientsView.vue`
    - Колонки: ФИО, Дата рождения, Email, Телефон, Бренд, Последний заказ.
    - Поиск по ФИО/email/телефону.
    - Колонка "Мастер" удалена.

