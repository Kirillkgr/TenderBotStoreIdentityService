# ER‑схема (упрощённо)

Ниже — упрощённая модель основных сущностей. Она помогает понять, как изолируются данные между владельцами (master) и
брендами.

- MasterAccount (владелец)
    - id, name, status
- Brand (бренд)
    - id, name, organizationName, master_id → MasterAccount
- User (пользователь)
    - id, username, email, passwordHash
    - связь многие‑ко‑многим с Brand (через membership)
- UserMembership (членство пользователя)
    - id, user_id → User, master_id → MasterAccount, brand_id → Brand?, pickup_point_id → PickupPoint?
- PickupPoint (локация/пункт выдачи)
    - id, brand_id → Brand
- GroupTag (группы товаров, древовидная структура)
    - id, brand_id → Brand, parent_id → GroupTag?, level, path
- Product (товар)
    - id, brand_id → Brand, group_tag_id → GroupTag?, price, promoPrice, visible
- Order (заказ)
    - id, client_id → User, brand_id → Brand, total, deliveryMode

Все связи построены так, чтобы на каждом уровне был «ключ к владельцу» через master → brand → (groupTag/product/order).
