# Warehouse Page: Required API (Spec)

This document summarizes the API required by the Warehouse page to render and manage warehouses together with their
ingredients and related reference data.

Notes:

- Warehouses backend is implemented already.
- Units and Suppliers are implemented on the frontend and have corresponding backend.
- Stock (ingredient quantities per warehouse) and Supplies (incoming deliveries) are outlined as a proposed contract for
  the next tasks.

## Auth and RBAC

- All endpoints are under authenticated scope `/auth/v1/...`.
- Context is scoped by current `masterId` (via cookies/JWT), so all list/search requests return only data for the
  current master.
- RBAC:
    - Read: typically `OWNER`, `ADMIN`, `COOK`, `CASHIER` (where applicable).
    - Mutations (create/update/delete): `OWNER`, `ADMIN`.
- Common errors:
    - 400 Bad Request: validation errors
    - 401 Unauthorized: no auth
    - 403 Forbidden: insufficient role
    - 404 Not Found: resource not found or out of scope
    - 409 Conflict: unique constraint (e.g., duplicate name per master)

---

## 1) Warehouses

Path: `/auth/v1/inventory/warehouses`

- GET List Warehouses
    - Roles: OWNER/ADMIN/COOK/CASHIER
    - Query: none (client-side search on name)
    - Response: `200 OK`
      ```json
      [
        { "id": 1, "name": "Main" },
        { "id": 2, "name": "Bar" }
      ]
      ```

- POST Create Warehouse
    - Roles: OWNER/ADMIN
    - Body:
      ```json
      { "name": "New Warehouse" }
      ```
    - Responses:
        - `201 Created`: `{ "id": 3, "name": "New Warehouse" }`
        - `400` if name blank/too long
        - `409` if name already exists for current master (case-insensitive)

- PUT Update Warehouse
    - Roles: OWNER/ADMIN
    - Path: `/{id}`
    - Body:
      ```json
      { "name": "Renamed" }
      ```
    - Responses:
        - `200 OK`: `{ "id": 3, "name": "Renamed" }`
        - `404` if not found (other master or missing)
        - `400` validation
        - `409` duplicate name

- DELETE Warehouse
    - Roles: OWNER/ADMIN
    - Path: `/{id}`
    - Responses: `204 No Content` or `404`

Uniqueness: unique `(master_id, name)` enforced (DB + service).

---

## 2) Units (reference)

Path: `/auth/v1/inventory/units`

- GET List Units
    - Roles: OWNER/ADMIN/COOK/CASHIER
    - Response: `200 OK`
      ```json
      [
        { "id": 1, "name": "Килограмм", "shortName": "кг" },
        { "id": 2, "name": "Литр", "shortName": "л" }
      ]
      ```

- PUT/POST/DELETE (if used in UI) — Roles: OWNER/ADMIN, standard 400/404/409 handling.

---

## 3) Ingredients

Path: `/auth/v1/inventory/ingredients`

- GET List Ingredients
    - Roles: OWNER/ADMIN/COOK/CASHIER
    - Query (optional): `q` (search by name), paging later
    - Response: `200 OK`
      ```json
      [
        { "id": 10, "name": "Сахар", "unitId": 1, "packageSize": 1.0, "notes": "Белый" },
        { "id": 11, "name": "Молоко", "unitId": 2, "packageSize": 1.0 }
      ]
      ```

- POST/PUT/DELETE (owner/admin) — standard validation and errors.

---

## 4) Stock (Ingredients per Warehouse)

Purpose: quantities of ingredients per warehouse.
Path (proposed): `/auth/v1/inventory/stock`

- GET Stock by Warehouse
    - Roles: OWNER/ADMIN/COOK/CASHIER
    - Query: `warehouseId` (required), optional `q` (ingredient name contains), paging later
    - Response: `200 OK`
      ```json
      [
        {
          "ingredientId": 10,
          "ingredientName": "Сахар",
          "unitId": 1,
          "unitName": "Килограмм",
          "qty": 42.500
        },
        {
          "ingredientId": 11,
          "ingredientName": "Молоко",
          "unitId": 2,
          "unitName": "Литр",
          "qty": 18.000
        }
      ]
      ```

- POST Adjust/Upsert (optional, if needed before supplies)
    - Roles: OWNER/ADMIN
    - Body (proposed):
      ```json
      {
        "warehouseId": 1,
        "ingredientId": 10,
        "qty": 5.000
      }
      ```
    - Responses: `200 OK` or `201 Created`; errors: 400/404

Notes: In future, direct stock adjustments may be replaced by documented movements (supplies/transfer/write-off).

---

## 5) Suppliers (reference for supplies)

Path: `/auth/v1/inventory/suppliers`

- GET List Suppliers
    - Roles: OWNER/ADMIN/COOK/CASHIER (read-only is fine)
    - Response: `200 OK`
      ```json
      [
        { "id": 1, "name": "ООО Поставки", "phone": "+7...", "email": "...", "address": "..." }
      ]
      ```

- POST/PUT/DELETE (owner/admin) — standard validation and errors.

---

## 6) Supplies (Incoming Deliveries) — Proposed Contract

Purpose: document-based inventory increase, linked to a supplier and warehouse.
Base Path: `/auth/v1/inventory/supplies`

- GET List Supplies
    - Roles: OWNER/ADMIN/COOK/CASHIER (read)
    - Query: `warehouseId?`, `supplierId?`, `from?`, `to?`, `q?` (doc number/notes), paging later
    - Response (example):
      ```json
      [
        {
          "id": 100,
          "docNo": "IN-2025-0001",
          "date": "2025-10-12T10:00:00",
          "warehouseId": 1,
          "warehouseName": "Main",
          "supplierId": 1,
          "supplierName": "ООО Поставки",
          "status": "POSTED",
          "lines": 3,
          "totalItems": 14.00
        }
      ]
      ```

- GET Supply by Id: `/{id}`
    - Roles: OWNER/ADMIN/COOK/CASHIER (read)
    - Response (example):
      ```json
      {
        "id": 100,
        "docNo": "IN-2025-0001",
        "date": "2025-10-12T10:00:00",
        "warehouseId": 1,
        "supplierId": 1,
        "status": "DRAFT",
        "notes": "",
        "items": [
          { "ingredientId": 10, "qty": 10.000 },
          { "ingredientId": 11, "qty": 4.000 }
        ]
      }
      ```

- POST Create Draft Supply
    - Roles: OWNER/ADMIN
    - Body:
      ```json
      {
        "warehouseId": 1,
        "supplierId": 1,
        "date": "2025-10-12T10:00:00",
        "notes": "",
        "items": [ { "ingredientId": 10, "qty": 10.000 } ]
      }
      ```
    - Response: `201 Created` with created entity

- PUT Update Draft Supply: `/{id}` (items and header)
    - Roles: OWNER/ADMIN
    - Response: `200 OK`, errors: 400/404

- POST Post Supply (apply to stock): `/{id}/post`
    - Roles: OWNER/ADMIN
    - Effect: increases stock quantities per (warehouseId, ingredientId)
    - Responses: `200 OK`, errors: 400 if not DRAFT, 404 if scope mismatch

- POST Cancel Supply: `/{id}/cancel`
    - Roles: OWNER/ADMIN
    - Effect: revert posting (if posted), or cancel draft
    - Responses: `200 OK`, errors: 400/404

- DELETE Draft Supply: `/{id}`
    - Roles: OWNER/ADMIN
    - Response: `204 No Content`, errors: 400 if posted, 404

Validation examples:

- Ingredient and Unit must belong to current master.
- Quantities must be positive decimals (`DECIMAL(18,6)`).
- Warehouse must be accessible (current master).

---

## Client Flow for Warehouse Page

1. Load base data (parallel):
    - GET `/auth/v1/inventory/warehouses`
    - GET `/auth/v1/inventory/units`
2. When a warehouse is selected:
    - GET `/auth/v1/inventory/stock?warehouseId={id}` (with optional `q` for ingredient name)
3. CRUD operations (OWNER/ADMIN):
    - Create/Update/Delete warehouse via respective endpoints.
    - For inventory operations, use Supplies (document-based flow) when implemented.
4. Show API error messages from `{ message }` field to user (409/400/403/404).

---

## Open Points / Next Steps

- Define and implement backend controllers/services for Ingredients and Stock (read first, then mutations via Supplies).
- Implement Supplies domain (entities, service, controller, integration tests).
- Add pagination/filtering for list endpoints as data grows.
