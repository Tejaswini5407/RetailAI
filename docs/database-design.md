# RetailAI — Database Design & Multi-Tenant Persistence Architecture

## 1. Overview & Purpose
Phase 7 establishes the foundational PostgreSQL database persistence layer for RetailAI. RetailAI is designed as a multi-tenant retail demand forecasting and inventory optimization SaaS platform. Every core domain entity is scoped to a specific `Organization` (tenant), guaranteeing strict logical data isolation across retailers.

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

| Entity Table | Primary Key | Parent Reference | Key Unique Constraints |
| :--- | :--- | :--- | :--- |
| `organizations` | `id` (BIGSERIAL) | None | `code` (VARCHAR 50) |
| `users` | `id` (BIGSERIAL) | `organization_id` | `email` (VARCHAR 255) |
| `stores` | `id` (BIGSERIAL) | `organization_id` | `(organization_id, store_code)` |
| `products` | `id` (BIGSERIAL) | `organization_id` | `(organization_id, sku)` |
| `sales` | `id` (BIGSERIAL) | `organization_id`, `store_id`, `product_id` | `(organization_id, store_id, product_id, sale_date)` |
| `inventory` | `id` (BIGSERIAL) | `organization_id`, `store_id`, `product_id` | `(organization_id, store_id, product_id)` |

---

## 4. Grains & Core Constraints

### 4.1 Sales Grain
- **Natural Grain:** `organization_id + store_id + product_id + sale_date`
- **Normalization:** Normalized daily observations (one row per store/product/date). Wide $d\_1 \dots d\_n$ format from development datasets is converted prior to database storage.
- **Constraints:** `units_sold >= 0`, `unit_price >= 0`.

### 4.2 Inventory Grain
- **Natural Grain:** `organization_id + store_id + product_id`
- **Fields:** `quantity_on_hand` (currently available stock) and `quantity_on_order` (replenishment stock in transit).
- **Constraints:** `quantity_on_hand >= 0`, `quantity_on_order >= 0`.

---

## 5. Indexing Strategy
To optimize tenant-scoped queries, the following indexes are applied:

1. `idx_users_org_id`: `users(organization_id)`
2. `idx_stores_org_code`: `stores(organization_id, store_code)`
3. `idx_products_org_sku`: `products(organization_id, sku)`
4. `idx_sales_org_store_prod_date`: `sales(organization_id, store_id, product_id, sale_date)`
5. `idx_inventory_org_store_prod`: `inventory(organization_id, store_id, product_id)`

---

## 6. Flyway Database Migrations
- **Location:** `backend/src/main/resources/db/migration/`
- **V1 Script:** `V1__initial_schema.sql` automatically creates all tables, check constraints, composite unique keys, foreign keys (`ON DELETE CASCADE`), and performance indexes.

---

## 7. Environment Configuration
Spring Boot connects to PostgreSQL via configurable environment variables:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/retailai_db}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

---

## 8. Local PostgreSQL Setup
1. PostgreSQL 17 server running on port `5432`.
2. Database name: `retailai_db`.
3. User credentials: configured safely via environment variables or local properties.

---

## 9. Known Limitations
- Authorization and JWT multi-tenant interceptors are scheduled for future security phases.
- Database tables are partitioned logically via `organization_id`; schema-per-tenant or database-per-tenant routing can be added if required for enterprise isolation.
