-- Event Sourcing Core Tables
-- Based on PostgreSQL Event Sourcing pattern

-- Aggregate table for version tracking and optimistic concurrency control
CREATE TABLE IF NOT EXISTS es_aggregate (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_es_aggregate_type ON es_aggregate(aggregate_type);

-- Event table - append-only event store
CREATE TABLE IF NOT EXISTS es_event (
    id BIGSERIAL PRIMARY KEY,
    transaction_id TEXT NOT NULL,
    aggregate_id UUID NOT NULL REFERENCES es_aggregate(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    json_data JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_es_event_aggregate_version UNIQUE (aggregate_id, version)
);

CREATE INDEX IF NOT EXISTS idx_es_event_aggregate_id ON es_event(aggregate_id);
CREATE INDEX IF NOT EXISTS idx_es_event_aggregate_version ON es_event(aggregate_id, version);
CREATE INDEX IF NOT EXISTS idx_es_event_transaction_id ON es_event(transaction_id);
CREATE INDEX IF NOT EXISTS idx_es_event_created_at ON es_event(created_at);

-- Aggregate snapshot table for optimization
CREATE TABLE IF NOT EXISTS es_aggregate_snapshot (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id UUID NOT NULL REFERENCES es_aggregate(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    json_data JSONB NOT NULL,
    CONSTRAINT uk_es_aggregate_snapshot_aggregate_version UNIQUE (aggregate_id, version)
);

CREATE INDEX IF NOT EXISTS idx_es_aggregate_snapshot_aggregate_id ON es_aggregate_snapshot(aggregate_id);
CREATE INDEX IF NOT EXISTS idx_es_aggregate_snapshot_version ON es_aggregate_snapshot(aggregate_id, version);

-- Event subscription table for asynchronous event processing
CREATE TABLE IF NOT EXISTS es_event_subscription (
    subscription_name VARCHAR(255) PRIMARY KEY,
    last_transaction_id TEXT,
    last_event_id BIGINT
);
