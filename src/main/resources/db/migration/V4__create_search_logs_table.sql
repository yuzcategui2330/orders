CREATE TABLE search_logs (
    id BIGSERIAL PRIMARY KEY,
    search_term VARCHAR(200) NOT NULL,
    result_count BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_search_logs_created_at ON search_logs (created_at);
