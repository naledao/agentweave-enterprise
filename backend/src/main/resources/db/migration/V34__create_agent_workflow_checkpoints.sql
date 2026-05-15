CREATE TABLE agent_workflow_checkpoints (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL,
    step_index INTEGER NOT NULL DEFAULT 0,
    node_name VARCHAR(80) NOT NULL,
    state_version INTEGER NOT NULL DEFAULT 1,
    state_payload TEXT NOT NULL,
    checksum VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_agent_workflow_checkpoints_run_id
        FOREIGN KEY (run_id) REFERENCES agent_runs(id) ON DELETE CASCADE
);

CREATE INDEX idx_agent_workflow_checkpoints_run_created_at
    ON agent_workflow_checkpoints(run_id, created_at DESC);

CREATE INDEX idx_agent_workflow_checkpoints_run_step
    ON agent_workflow_checkpoints(run_id, step_index);
