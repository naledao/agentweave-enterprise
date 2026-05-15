CREATE TABLE agent_steps (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL,
    step_index INTEGER NOT NULL,
    step_type VARCHAR(40) NOT NULL,
    node_name VARCHAR(80),
    status VARCHAR(40) NOT NULL DEFAULT 'pending',
    input_summary TEXT,
    output_summary TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    error_code VARCHAR(80),
    error_message VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_agent_steps_run_id
        FOREIGN KEY (run_id) REFERENCES agent_runs(id) ON DELETE CASCADE,
    CONSTRAINT ck_agent_steps_status
        CHECK (status IN ('pending', 'running', 'succeeded', 'failed', 'skipped')),
    CONSTRAINT ck_agent_steps_step_type
        CHECK (step_type IN ('planning', 'rag_search', 'graph_rag_search', 'tool_call', 'review', 'final_answer', 'human_approval', 'checkpoint', 'error')),
    CONSTRAINT uq_agent_steps_run_step_index
        UNIQUE (run_id, step_index)
);

CREATE INDEX idx_agent_steps_run_id_step_index
    ON agent_steps(run_id, step_index);

CREATE INDEX idx_agent_steps_run_id_status
    ON agent_steps(run_id, status);

CREATE INDEX idx_agent_steps_step_type
    ON agent_steps(step_type);
