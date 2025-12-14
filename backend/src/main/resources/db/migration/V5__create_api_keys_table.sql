-- API Keys table for storing user API keys
-- API keys are encrypted/hashed for security

CREATE TABLE IF NOT EXISTS api_keys (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    key_value TEXT NOT NULL, -- Encrypted/hashed API key value
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_api_keys_user_name UNIQUE (user_id, name)
);

CREATE INDEX IF NOT EXISTS idx_api_keys_user_id ON api_keys(user_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_name ON api_keys(name);
