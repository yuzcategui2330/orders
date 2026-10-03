CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    short_name VARCHAR(80) NOT NULL,
    category VARCHAR(32) NOT NULL,
    stock_quantity INTEGER NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_products_category CHECK (category IN ('MEDICINES', 'HYGYENE', 'PERSONAL_CARE'))
);

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_products_stock_quantity ON products (stock_quantity);
CREATE INDEX idx_products_name ON products (name);
CREATE INDEX idx_products_name_trgm ON products USING gin (lower(name) gin_trgm_ops);
CREATE INDEX idx_products_short_name_trgm ON products USING gin (lower(short_name) gin_trgm_ops);
