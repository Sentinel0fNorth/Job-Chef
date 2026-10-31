CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash CHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    token_expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE TABLE enrollment_codes (
    token_hash CHAR(64) PRIMARY KEY,
    remaining_uses INTEGER NOT NULL CHECK (remaining_uses >= 0),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE resumes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    client_key VARCHAR(80) NOT NULL,
    version INTEGER NOT NULL CHECK (version > 0),
    display_name VARCHAR(120) NOT NULL,
    sanitized_text TEXT NOT NULL CHECK (length(sanitized_text) BETWEEN 1 AND 50000),
    parsed_content JSONB,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (device_id, version),
    UNIQUE (device_id, client_key)
);
CREATE INDEX resumes_device_active_idx ON resumes(device_id, active);

CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id VARCHAR(160) NOT NULL UNIQUE,
    title VARCHAR(240) NOT NULL,
    company VARCHAR(200) NOT NULL,
    location VARCHAR(240) NOT NULL,
    description TEXT NOT NULL CHECK (length(description) <= 20000),
    salary_min NUMERIC(14,2),
    salary_max NUMERIC(14,2),
    currency CHAR(3),
    redirect_url TEXT NOT NULL,
    work_mode VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    skills JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    refreshed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE decisions (
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL CHECK (status IN ('SAVED', 'REJECTED', 'UNSEEN')),
    revision BIGINT NOT NULL CHECK (revision > 0),
    idempotency_key VARCHAR(80) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (device_id, job_id),
    UNIQUE (device_id, idempotency_key)
);

CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    resume_id UUID REFERENCES resumes(id) ON DELETE RESTRICT,
    resume_version INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('WAITING_FOR_RESUME', 'QUEUED', 'PROCESSING', 'READY', 'FAILED', 'QUOTA_BLOCKED', 'CANCELLED')),
    generation_version INTEGER NOT NULL DEFAULT 1,
    regeneration_day DATE NOT NULL DEFAULT CURRENT_DATE,
    regenerations_today INTEGER NOT NULL DEFAULT 0 CHECK (regenerations_today >= 0),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    lease_until TIMESTAMPTZ,
    lease_token UUID,
    error_code VARCHAR(60),
    error_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (device_id, job_id)
);
CREATE INDEX applications_device_created_idx ON applications(device_id, created_at DESC);

CREATE TABLE application_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    version INTEGER NOT NULL CHECK (version > 0),
    resume_content JSONB,
    cover_letter TEXT,
    is_demo BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (application_id, version)
);

CREATE TABLE quota_usage (
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    usage_day DATE NOT NULL,
    new_applications INTEGER NOT NULL DEFAULT 0 CHECK (new_applications >= 0),
    regeneration_count INTEGER NOT NULL DEFAULT 0 CHECK (regeneration_count >= 0),
    PRIMARY KEY (device_id, usage_day)
);

CREATE TABLE outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(60) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX outbox_pending_idx ON outbox(next_attempt_at, created_at) WHERE published_at IS NULL;
