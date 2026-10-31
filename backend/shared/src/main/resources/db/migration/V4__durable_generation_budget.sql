CREATE TABLE global_generation_usage (
    usage_day DATE PRIMARY KEY,
    provider_calls INTEGER NOT NULL DEFAULT 0 CHECK (provider_calls >= 0),
    reserved_tokens INTEGER NOT NULL DEFAULT 0 CHECK (reserved_tokens >= 0)
);
