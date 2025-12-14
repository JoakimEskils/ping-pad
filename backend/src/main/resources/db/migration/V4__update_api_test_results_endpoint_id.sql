-- Migration to update api_test_results to use UUID for endpoint_id
-- This aligns with the event sourcing migration to UUID-based endpoints

-- Add new UUID column for endpoint_id
ALTER TABLE api_test_results ADD COLUMN IF NOT EXISTS endpoint_uuid_id UUID;

-- Note: We can't automatically migrate existing data since the old endpoint_id references Long IDs
-- For new records, endpoint_uuid_id will be used
-- The old endpoint_id column can be dropped later after all data is migrated

-- Make endpoint_uuid_id NOT NULL for new records (but allow NULL for migration period)
-- We'll make it NOT NULL in a later migration after all old data is migrated
