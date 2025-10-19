# Frontend — оглавление и практики

## Оглавление

- Базовый HTTP‑клиент (Axios, withCredentials)
- Аутентификация и обновление токена (login/refresh)
- Контекст (tenant/master/brand)
- Работа с куками (guest cart, refreshToken)
- Инвентарь: страницы и вызовы API
- Уведомления и long‑poll

## Базовый HTTP‑клиент

- Используйте единый экземпляр Axios с `baseURL` и `withCredentials: true`.
- Следите, чтобы origin фронта точно совпадал с одним из `allowedOrigins` на backend (без завершающего `/`).

```ts
import axios from 'axios';
export const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE, withCredentials: true });
```

## Аутентификация и обновление токена

- `POST /auth/v1/login` — возвращает профиль и устанавливает HttpOnly `refreshToken`.
- `POST /auth/v1/refresh` — браузер отправляет `refreshToken` из cookie; в dev по HTTP включите `app.cookie.secure=false`.
- Access‑токен храните в памяти (например, Pinia/Vuex) и добавляйте в `Authorization: Bearer ...`.

## Контекст (tenant/master/brand)

- Для защищённых эндпоинтов требуется валидный контекст; без него списки будут пустыми.
- Контекст переключается на фронте и попадает в куки/хранилище согласно настройкам приложения.

## Работа с куками

- `cart_token` — анонимная корзина; при логине объединяется с пользовательской.
- `refreshToken` — HttpOnly, SameSite=None; флаг `Secure` управляется `app.cookie.secure` на сервере.

## Инвентарь: страницы и API

- См. [Страница остатков (Stock)](wiki/frontend-inventory-stock) для страницы остатков (фильтры и UX).
- CRUD справочников: Ingredients/Units/Warehouses/Suppliers/Packaging.
- Все ручки собраны в Swagger группе `inventory`.

## Уведомления и long‑poll

- Для лонгполлинга обязательно отправлять куки/авторизацию, иначе события не будут доставляться.
