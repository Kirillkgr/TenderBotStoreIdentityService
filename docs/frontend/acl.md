# ACL (Role-based UI visibility)

Этот документ описывает, как управлять видимостью и доступностью элементов интерфейса на основе ролей пользователя.

## Роли

- Глобальные: ADMIN, OWNER, USER
- Membership: COOK, CASHIER, CLIENT

Все роли собираются в `auth.roles` (Pinia), и используются как единый источник прав на фронтенде.

## Composable: useAcl()

Файл: `Front/src/composables/useAcl.js`

```js
import {useAcl} from '@/composables/useAcl';

const {can, any, all, roles} = useAcl();

// Примеры
any(['ADMIN', 'OWNER']); // хотя бы одна роль
all(['ADMIN', 'OWNER']); // обе роли
can(['COOK', 'CASHIER']); // любая из ролей
can({all: ['CLIENT', 'COOK']}); // все роли
can({any: ['ADMIN', 'OWNER']}); // любая роль
```

## Директива: v-can

Файл: `Front/src/directives/can.js`
Регистрация: глобально в `Front/src/main.js` — `app.directive('can', canDirective)`

Синтаксис:

```vue
<!-- Скрыть, если нет ролей -->
<button v-can="{ any: ['ADMIN','OWNER'], mode: 'hide' }">Создать бренд</button>

<!-- Дизейблить, если нет ролей (с подсказкой) -->
<button v-can="{ any: ['ADMIN'], mode: 'disable', tooltip: 'Нет прав' }">Сохранить</button>

<!-- Короткая форма: массив ролей => режим по умолчанию hide -->
<button v-can="['COOK']">Кухня</button>

<!-- Ссылка, видимая по membership, переключает контекст перед переходом -->
<a @click.prevent="ensureRoleAndGo('CASHIER', '/admin/orders', ['ADMIN','OWNER','CASHIER'])">Заказы</a>
```

Поведение:

- `mode: 'hide'` — элемент скрывается через `display: none`.
- `mode: 'disable'` — элемент получает `pointer-events: none`, `opacity: .5`, `aria-disabled` и `title` с подсказкой.

## Где применяется

- `AppHeader.vue`: ссылки навигации по ролям (Админ — ADMIN/OWNER; Кухня — COOK; Касса — CASHIER).
- `AdminView.vue`: кнопки создания бренда/тега/товара и действия по тегам/товарам — ADMIN/OWNER.
- `AdminOrdersView.vue`: смена статуса, сообщения, отзывы — ADMIN/OWNER.
    - Кнопка "Сохранить" расширена до `['ADMIN','OWNER','CASHIER']`.

## Рекомендации

- По умолчанию используйте `mode: 'hide'` для чувствительных элементов.
- Для обучающих сценариев используйте `mode: 'disable'` с `tooltip`.
- Избегайте дублирования логики: используйте `v-can` и/или `useAcl()` вместо ручных проверок в каждом компоненте.

## Пример использования useAcl()

```js
import {useAcl} from '@/composables/useAcl';

const {can, any, all, roles} = useAcl();

if (can({any: ['ADMIN', 'OWNER']})) {
    // показать админ-раздел
}

if (any(['CASHIER', 'COOK'])) {
    // показать кнопки для персонала
}
```

## Паттерн автопереключения контекста (membership)

Используется для ссылок, видимых при наличии подходящего membership. Перед навигацией выполняется `switchContext` и
ожидание обновления ролей:

```js
// AppHeader.vue (фрагмент)
async function ensureRoleAndGo(requiredRole, path, anyOf = null) {
    const target = Array.isArray(anyOf) && anyOf.length ? anyOf : [requiredRole];
    // если нужной роли нет — ищем membership с такой ролью, делаем switchContext,
    // ждём обновления roles в authStore (до ~1.5с), затем router.push(path)
}
```

## Доступность (A11y)

- Для `mode: 'disable'` директива выставляет `aria-disabled="true"`, `pointer-events: none`, `opacity: .5`, и `title`.
- Следите, чтобы элементы в disabled‑состоянии не ломали навигацию с клавиатуры.
- Для `mode: 'hide'` элемент полностью скрыт и для скринридеров.
- Используйте понятные подсказки: например, «Недостаточно прав».

## Тестирование

- Компонентные тесты `AppHeader.roles.spec.js` проверяют видимость ссылок по ролям.
- `AppHeader.navigate.spec.js` проверяет автопереключение контекста и переход к "Заказам".
- Юнит-тест `useAcl.spec.js` проверяет логику `useAcl()`.
- `AdminOrders.roles.spec.js` проверяет, что у `CASHIER` кнопка "Сохранить" активна.
- По необходимости добавляйте тесты на конкретные компоненты/кнопки с `v-can`.
