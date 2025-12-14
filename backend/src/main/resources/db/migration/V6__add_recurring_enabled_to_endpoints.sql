-- Migration to add recurring_enabled column to api_endpoints table
-- This allows endpoints to be automatically tested every 5 minutes

ALTER TABLE api_endpoints ADD COLUMN IF NOT EXISTS recurring_enabled BOOLEAN DEFAULT FALSE;

-- Create index for efficient querying of recurring endpoints
CREATE INDEX IF NOT EXISTS idx_api_endpoints_recurring_enabled ON api_endpoints(recurring_enabled) WHERE recurring_enabled = TRUE;
