-- Migration to add recurring_interval and last_run_at columns to api_endpoints table
-- This allows endpoints to be automatically tested at different intervals

ALTER TABLE api_endpoints ADD COLUMN IF NOT EXISTS recurring_interval VARCHAR(20) DEFAULT NULL;
ALTER TABLE api_endpoints ADD COLUMN IF NOT EXISTS last_run_at TIMESTAMP DEFAULT NULL;

-- Update existing recurring_enabled endpoints to have default interval of 30s
UPDATE api_endpoints SET recurring_interval = '30s' WHERE recurring_enabled = TRUE AND recurring_interval IS NULL;

-- Create index for efficient querying of recurring endpoints
CREATE INDEX IF NOT EXISTS idx_api_endpoints_recurring_interval ON api_endpoints(recurring_interval) WHERE recurring_interval IS NOT NULL;
