-- =============================================================================
-- RetailAI — Flyway Migration V2: Import History and Forecast History Tables
-- Multi-Tenant Data Import Center & Forecast Accuracy Tracking Architecture
-- =============================================================================

-- 1. Import History Table
CREATE TABLE import_history (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    import_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    total_rows INTEGER NOT NULL DEFAULT 0,
    successful_rows INTEGER NOT NULL DEFAULT 0,
    failed_rows INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    error_summary TEXT,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_import_history_org ON import_history(organization_id);

-- 2. Forecast History Table
CREATE TABLE forecast_history (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    forecast_run_id VARCHAR(50) NOT NULL,
    forecast_date DATE NOT NULL,
    target_date DATE NOT NULL,
    horizon_days INTEGER NOT NULL DEFAULT 1,
    predicted_demand NUMERIC(10, 4) NOT NULL,
    rounded_demand INTEGER NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    model_version VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_forecast_history_org_store_prod ON forecast_history(organization_id, store_id, product_id);
CREATE INDEX idx_forecast_history_target ON forecast_history(target_date);
