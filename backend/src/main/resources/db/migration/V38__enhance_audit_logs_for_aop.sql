ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS resource_type VARCHAR(80),
    ADD COLUMN IF NOT EXISTS resource_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS duration_ms BIGINT,
    ADD COLUMN IF NOT EXISTS request_summary VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS response_summary VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS error_message VARCHAR(500);

UPDATE audit_logs
SET resource_type = COALESCE(resource_type, target_type),
    resource_id = COALESCE(resource_id, target_id),
    error_message = COALESCE(error_message, reason)
WHERE resource_type IS NULL
   OR resource_id IS NULL
   OR error_message IS NULL;

CREATE INDEX IF NOT EXISTS idx_audit_logs_event_created_at
    ON audit_logs(event_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_created_at
    ON audit_logs(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_resource
    ON audit_logs(resource_type, resource_id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_result_created_at
    ON audit_logs(result, created_at DESC);
