-- =============================================================================
-- RetailAI — Phase 7 Initial Relational Database Schema (Flyway Migration V1)
-- Multi-Tenant SaaS Architecture for Demand Forecasting & Inventory Intelligence
-- =============================================================================

-- 1. Organizations Table (Tenant Entity)
CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Users Table (Tenant-Scoped User Accounts)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Stores Table (Tenant-Scoped Retail Outlets)
CREATE TABLE stores (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    state VARCHAR(10) NOT NULL,
    city VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_stores_org_store_code UNIQUE (organization_id, store_code)
);

-- 4. Products Table (Tenant-Scoped Catalog Items)
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    sku VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_products_org_sku UNIQUE (organization_id, sku)
);

-- 5. Sales Table (Daily Point-of-Sale Observations per Store/Product)
CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sale_date DATE NOT NULL,
    units_sold INTEGER NOT NULL CONSTRAINT chk_sales_units_sold CHECK (units_sold >= 0),
    unit_price NUMERIC(10, 2) CONSTRAINT chk_sales_unit_price CHECK (unit_price IS NULL OR unit_price >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sales_org_store_prod_date UNIQUE (organization_id, store_id, product_id, sale_date)
);

-- 6. Inventory Table (Current Stock Positions per Store/Product)
CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity_on_hand INTEGER NOT NULL CONSTRAINT chk_inventory_qty_on_hand CHECK (quantity_on_hand >= 0),
    quantity_on_order INTEGER NOT NULL CONSTRAINT chk_inventory_qty_on_order CHECK (quantity_on_order >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_inventory_org_store_prod UNIQUE (organization_id, store_id, product_id)
);

-- =============================================================================
-- INDEXING STRATEGY
-- =============================================================================
CREATE INDEX idx_users_org_id ON users(organization_id);
CREATE INDEX idx_stores_org_code ON stores(organization_id, store_code);
CREATE INDEX idx_products_org_sku ON products(organization_id, sku);
CREATE INDEX idx_sales_org_store_prod_date ON sales(organization_id, store_id, product_id, sale_date);
CREATE INDEX idx_inventory_org_store_prod ON inventory(organization_id, store_id, product_id);
