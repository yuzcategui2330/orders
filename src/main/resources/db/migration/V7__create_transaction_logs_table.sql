CREATE TABLE transaction_logs (
    id BIGSERIAL PRIMARY KEY,
    transaction_id UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type VARCHAR(40) NOT NULL,
    action VARCHAR(20) NOT NULL,
    module VARCHAR(40) NOT NULL,
    request_payload JSONB,
    response_payload JSONB,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    CONSTRAINT ck_transaction_logs_status CHECK (status IN ('SUCCESS', 'ERROR'))
);

CREATE INDEX idx_transaction_logs_transaction_id ON transaction_logs (transaction_id);
CREATE INDEX idx_transaction_logs_type ON transaction_logs (type);
CREATE INDEX idx_transaction_logs_created_at ON transaction_logs (created_at DESC);
CREATE INDEX idx_transaction_logs_request_payload ON transaction_logs USING GIN (request_payload jsonb_path_ops);
CREATE INDEX idx_transaction_logs_response_payload ON transaction_logs USING GIN (response_payload jsonb_path_ops);
CREATE INDEX idx_transaction_logs_request_order_id ON transaction_logs ((request_payload ->> 'order_id'));
CREATE INDEX idx_transaction_logs_request_client_id ON transaction_logs ((request_payload ->> 'client_id'));
