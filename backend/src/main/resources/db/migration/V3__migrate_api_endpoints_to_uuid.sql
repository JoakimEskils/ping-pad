-- Migration to convert api_endpoints table to use UUID for event sourcing
-- This migration handles the transition from Long ID to UUID ID

-- First, add a new UUID column
ALTER TABLE api_endpoints ADD COLUMN IF NOT EXISTS uuid_id UUID;

-- Generate UUIDs for existing records
UPDATE api_endpoints SET uuid_id = gen_random_uuid() WHERE uuid_id IS NULL;

-- Make uuid_id NOT NULL
ALTER TABLE api_endpoints ALTER COLUMN uuid_id SET NOT NULL;

-- Create unique constraint on uuid_id
CREATE UNIQUE INDEX IF NOT EXISTS idx_api_endpoints_uuid_id ON api_endpoints(uuid_id);

-- Note: The old 'id' column (Long) is kept for backward compatibility
-- You can drop it later after migrating all references
-- ALTER TABLE api_endpoints DROP COLUMN id;

-- Update api_test_results to reference uuid_id if needed
-- For now, we'll keep both references during migration
-- ALTER TABLE api_test_results ADD COLUMN endpoint_uuid_id UUID REFERENCES api_endpoints(uuid_id);
