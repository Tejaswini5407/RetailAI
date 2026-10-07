# RetailAI — Database Design & Multi-Tenant Persistence Architecture

## 1. Overview & Purpose
Phase 7 establishes the foundational PostgreSQL database persistence layer for RetailAI. RetailAI is designed as a multi-tenant retail demand forecasting and inventory optimization SaaS platform. Every core domain entity is scoped to a specific `Organization` (tenant), guaranteeing strict logical data isolation and database-level integrity across retailers.

---

## 2. Multi-Tenant Architecture & Entity Relationships

### Text ER Diagram
```
Organization (Tenant Root)
 ├── Users (ADMIN, STORE_MANAGER, INVENTORY_MANAGER)
 ├── Stores (Physical Retail Outlets)
 ├── Products (SKUs / Catalog Items)
 ├── Sales (Daily Point-of-Sale Observations)
 └── Inventory (Current Stock Positions)
```

---

## 3. Entity & Schema Summary

| Entity Table | Primary Key | Parent Reference | Key Unique Constraints | Composite Foreign Keys |
| :--- | :--- | :--- | :--- | :--- |
| `organizations` | `id` (BIGSERIAL) | None | `code` (VARCHAR 50) | None |
| `users` | `id` (BIGSERIAL) | `organization_id` | `email` (VARCHAR 255) | None |
| `stores` | `id` (BIGSERIAL) | `organization_id` | `(organization_id, store_code)`, `(organization_id, id)` | None |
| `products` | `id` (BIGSERIAL) | `organization_id` | `(organization_id, sku)`, `(organization_id, id)` | None |
| `sales` | `id` (BIGSERIAL) | `organization_id`, `store_id`, `product_id` | `(organization_id, store_id, product_id, sale_date)` | `(organization_id, store_id) → stores(organization_id, id)`, `(organization_id, product_id) → products(organization_id, id)` |
| `inventory` | `id` (BIGSERIAL) | `organization_id`, `store_id`, `product_id` | `(organization_id, store_id, product_id)` | `(organization_id, store_id) → stores(organization_id, id)`, `(organization_id, product_id) → products(organization_id, id)` |

---

## 4. Cross-Tenant Integrity & Database Safeguards

### 4.1 Cross-Tenant Foreign Key Enforcement
To prevent cross-tenant reference defects (e.g. Organization A attempting to reference Organization B's store or product), composite foreign keys are enforced in PostgreSQL:
- `fk_sales_org_store`: `sales(organization_id, store_id) REFERENCES stores(organization_id, id)`
- `fk_sales_org_product`: `sales(organization_id, product_id) REFERENCES products(organization_id, id)`
- `fk_inventory_org_store`: `inventory(organization_id, store_id) REFERENCES stores(organization_id, id)`
- `fk_inventory_org_product`: `inventory(organization_id, product_id) REFERENCES products(organization_id, id)`

If a sale or inventory record attempts to reference a store or product belonging to a different organization, PostgreSQL immediately raises a `foreign_key_violation` (23503) and rejects the transaction.

### 4.2 Database CHECK Constraints
Authoritative integrity constraints defined directly in PostgreSQL DDL:
- `chk_sales_units_sold`: `units_sold >= 0`
- `chk_sales_unit_price`: `unit_price IS NULL OR unit_price >= 0`
- `chk_inventory_qty_on_hand`: `quantity_on_hand >= 0`
- `chk_inventory_qty_on_order`: `quantity_on_order >= 0`

---

## 5. Index Review & Optimization
PostgreSQL automatically creates unique B-tree indexes for all `UNIQUE` constraints. Redundant manual indexes were removed to prevent duplicate index overhead while retaining full query performance:
- `uk_stores_org_store_code`: Automatically creates unique B-tree index on `(organization_id, store_code)`.
- `uk_products_org_sku`: Automatically creates unique B-tree index on `(organization_id, sku)`.
- `uk_sales_org_store_prod_date`: Automatically creates unique B-tree index on `(organization_id, store_id, product_id, sale_date)`.
- `uk_inventory_org_store_prod`: Automatically creates unique B-tree index on `(organization_id, store_id, product_id)`.
- `idx_users_org_id`: Retained manual index on `users(organization_id)` to support organization-scoped user queries.

---

## 6. User Email Tenancy Decision
In RetailAI's SaaS architecture, user email (`users.email`) is kept globally unique (`UNIQUE(email)`). This allows users to authenticate uniquely across the platform while being linked to their respective `organization_id` for role-based tenant access.

---

## 7. Grains

### 7.1 Sales Grain
- **Natural Grain:** `organization_id + store_id + product_id + sale_date`
- **Normalization:** Normalized daily observations (one row per store/product/date).

### 7.2 Inventory Grain
- **Natural Grain:** `organization_id + store_id + product_id`
- **Fields:** `quantity_on_hand` (available stock) and `quantity_on_order` (replenishment stock in transit).

---

## 8. Flyway Database Migrations
- **Location:** `backend/src/main/resources/db/migration/`
- **V1 Script:** `V1__initial_schema.sql` automatically creates all tables, composite unique keys, composite foreign keys, check constraints, and performance indexes.

---

## 9. Environment Configuration
Spring Boot connects to PostgreSQL via configurable environment variables:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/retailai_db}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

---

## 10. Local PostgreSQL Setup
1. PostgreSQL 17 server running on port `5432`.
2. Database name: `retailai_db`.
3. User credentials: configured safely via environment variables or local properties.

---

## 11. Known Limitations
- Authorization and JWT multi-tenant interceptors are scheduled for Phase 8.
- Database tables are partitioned logically via `organization_id`; schema-per-tenant or database-per-tenant routing can be added if required for enterprise isolation.
