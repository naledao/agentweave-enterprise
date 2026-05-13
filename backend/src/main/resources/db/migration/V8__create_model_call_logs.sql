CREATE TABLE model_call_logs (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    message_id UUID NOT NULL REFERENCES conversation_messages(id) ON DELETE CASCADE,
    provider VARCHAR(80) NOT NULL,
    model VARCHAR(160) NOT NULL,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    latency_ms BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    error_message VARCHAR(500),
    trace_id VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_model_call_logs_conversation_created_at
    ON model_call_logs(conversation_id, created_at DESC);

CREATE INDEX idx_model_call_logs_message
    ON model_call_logs(message_id);
