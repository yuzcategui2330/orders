CREATE TABLE credit_cards (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(36) NOT NULL,
    card_number VARCHAR(512) NOT NULL,
    expiration_month SMALLINT NOT NULL,
    expiration_year SMALLINT NOT NULL,
    holder_name VARCHAR(120) NOT NULL,
    client_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_credit_cards_token UNIQUE (token),
    CONSTRAINT fk_credit_cards_client FOREIGN KEY (client_id) REFERENCES clients (id)
);

CREATE INDEX idx_credit_cards_client_id ON credit_cards (client_id);
