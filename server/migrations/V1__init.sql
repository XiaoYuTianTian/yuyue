-- 语曰 Yuyue schema (PostgreSQL / H2 PostgreSQL mode)

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    phone_hash VARCHAR(128) NOT NULL UNIQUE,
    phone_masked VARCHAR(32) NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS notebooks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(128) NOT NULL,
    slug VARCHAR(64) NOT NULL,
    color VARCHAR(32),
    icon VARCHAR(64),
    sort_order INT NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (user_id, slug)
);

CREATE TABLE IF NOT EXISTS entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    notebook_id UUID NOT NULL REFERENCES notebooks(id),
    entry_kind VARCHAR(32) NOT NULL DEFAULT 'note',
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    subject_key VARCHAR(128),
    polarity VARCHAR(16),
    supersedes_entry_id UUID REFERENCES entries(id),
    title VARCHAR(512) NOT NULL,
    body CLOB NOT NULL,
    raw_transcript CLOB,
    start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at TIMESTAMP WITH TIME ZONE,
    stated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    confidence REAL,
    source_quote CLOB,
    schema_version INT NOT NULL DEFAULT 1,
    idempotency_key VARCHAR(128),
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (user_id, idempotency_key)
);

CREATE TABLE IF NOT EXISTS async_jobs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    input CLOB NOT NULL,
    output CLOB,
    error CLOB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS user_daily_usage (
    user_id UUID NOT NULL REFERENCES users(id),
    usage_date DATE NOT NULL,
    ingest_count INT NOT NULL DEFAULT 0,
    recall_count INT NOT NULL DEFAULT 0,
    llm_tokens INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, usage_date)
);

CREATE TABLE IF NOT EXISTS sms_otp (
    phone_hash VARCHAR(128) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_entries_cal ON entries (user_id, notebook_id, start_at);
CREATE INDEX IF NOT EXISTS idx_entries_time ON entries (user_id, start_at);
CREATE INDEX IF NOT EXISTS idx_entries_subject ON entries (user_id, subject_key, stated_at);
CREATE INDEX IF NOT EXISTS idx_notebooks_user ON notebooks (user_id);
CREATE INDEX IF NOT EXISTS idx_jobs_user_status ON async_jobs (user_id, status);
