ALTER TABLE products
    ADD COLUMN price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN reserved_qty INTEGER NOT NULL DEFAULT 0;

ALTER TABLE products
    ADD CONSTRAINT ck_products_reserved_qty CHECK (reserved_qty >= 0 AND reserved_qty <= stock_quantity);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    delivered BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_client FOREIGN KEY (client_id) REFERENCES clients (id)
);

CREATE INDEX idx_orders_client_id ON orders (client_id);

CREATE TABLE order_details (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_details_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_details_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uk_order_details_order_product UNIQUE (order_id, product_id),
    CONSTRAINT ck_order_details_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_order_details_order_id ON order_details (order_id);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    amount NUMERIC(12, 2) NOT NULL,
    reference VARCHAR(64) NOT NULL,
    credit_card_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    order_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payments_reference UNIQUE (reference),
    CONSTRAINT fk_payments_credit_card FOREIGN KEY (credit_card_id) REFERENCES credit_cards (id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_payments_order_id ON payments (order_id);
