CREATE TABLE search_cache (
    cache_key CHAR(64) PRIMARY KEY,
    response_json JSONB NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX search_cache_expiry_idx ON search_cache(expires_at);
