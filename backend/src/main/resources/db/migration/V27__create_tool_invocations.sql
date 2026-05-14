CREATE TABLE tool_invocations (
    id UUID PRIMARY KEY,
    tool_code VARCHAR(120) NOT NULL,
    user_id UUID,
    username VARCHAR(80),
    conversation_id UUID,
    message_id UUID,
    input_summary TEXT,
    result_summary TEXT,
    status VARCHAR(40) NOT NULL,
    duration_ms BIGINT,
    error_message VARCHAR(500),
    trace_id VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    finished_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_tool_invocations_status
        CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED', 'DENIED', 'TIMEOUT'))
);

CREATE INDEX idx_tool_invocations_user_created_at
    ON tool_invocations(user_id, created_at DESC);

CREATE INDEX idx_tool_invocations_tool_created_at
    ON tool_invocations(tool_code, created_at DESC);

CREATE INDEX idx_tool_invocations_status_created_at
    ON tool_invocations(status, created_at DESC);

CREATE INDEX idx_tool_invocations_trace_id
    ON tool_invocations(trace_id);

CREATE INDEX idx_tool_invocations_conversation_message
    ON tool_invocations(conversation_id, message_id);
