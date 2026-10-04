CREATE INDEX idx_products_description_trgm ON products USING gin (lower(description) gin_trgm_ops);
