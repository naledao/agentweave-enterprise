CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    user_id UUID,
    username VARCHAR(80),
    target_type VARCHAR(80),
    target_id VARCHAR(120),
    action VARCHAR(120) NOT NULL,
    result VARCHAR(40) NOT NULL,
    reason VARCHAR(500),
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    trace_id VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_trace_id ON audit_logs(trace_id);
