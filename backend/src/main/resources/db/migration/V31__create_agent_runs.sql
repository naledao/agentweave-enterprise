CREATE TABLE agent_runs (
    id UUID PRIMARY KEY,
    conversation_id UUID,
    user_id UUID NOT NULL,
    trace_id VARCHAR(120),
    goal TEXT NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'created',
    current_step_index INTEGER NOT NULL DEFAULT 0,
    final_answer TEXT,
    error_code VARCHAR(80),
    error_message VARCHAR(500),
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_agent_runs_status
        CHECK (status IN ('created', 'planning', 'executing', 'reviewing', 'succeeded', 'failed', 'cancelled'))
);

CREATE INDEX idx_agent_runs_user_created_at
    ON agent_runs(user_id, created_at DESC);

CREATE INDEX idx_agent_runs_conversation_id
    ON agent_runs(conversation_id);

CREATE INDEX idx_agent_runs_status_created_at
    ON agent_runs(status, created_at DESC);

CREATE INDEX idx_agent_runs_trace_id
    ON agent_runs(trace_id);
