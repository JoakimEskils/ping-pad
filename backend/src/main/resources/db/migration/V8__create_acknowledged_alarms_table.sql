-- Migration to create acknowledged_alarms table
-- This table tracks which alarms/errors have been acknowledged by users

CREATE TABLE IF NOT EXISTS acknowledged_alarms (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id UUID NOT NULL,
    test_result_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    acknowledged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_acknowledged_alarm_endpoint FOREIGN KEY (endpoint_id) REFERENCES api_endpoints(uuid_id) ON DELETE CASCADE,
    CONSTRAINT fk_acknowledged_alarm_test_result FOREIGN KEY (test_result_id) REFERENCES api_test_results(id) ON DELETE CASCADE,
    CONSTRAINT fk_acknowledged_alarm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create index for efficient querying
CREATE INDEX IF NOT EXISTS idx_acknowledged_alarms_endpoint_id ON acknowledged_alarms(endpoint_id);
CREATE INDEX IF NOT EXISTS idx_acknowledged_alarms_user_id ON acknowledged_alarms(user_id);
CREATE INDEX IF NOT EXISTS idx_acknowledged_alarms_test_result_id ON acknowledged_alarms(test_result_id);

-- Unique constraint to prevent duplicate acknowledgments
CREATE UNIQUE INDEX IF NOT EXISTS idx_acknowledged_alarms_unique ON acknowledged_alarms(user_id, test_result_id);
