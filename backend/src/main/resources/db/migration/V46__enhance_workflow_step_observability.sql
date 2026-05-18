ALTER TABLE agent_steps
    ADD COLUMN trace_id VARCHAR(120),
    ADD COLUMN agent_role VARCHAR(40);

UPDATE agent_steps s
SET trace_id = r.trace_id
FROM agent_runs r
WHERE s.run_id = r.id
  AND s.trace_id IS NULL;

UPDATE agent_steps
SET agent_role = CASE
    WHEN step_type = 'PLANNING' THEN 'PLANNER'
    WHEN step_type IN ('REVIEW', 'FINAL_ANSWER') THEN 'REVIEWER'
    WHEN step_type = 'HUMAN_APPROVAL' THEN 'APPROVAL'
    WHEN step_type IN ('CHECKPOINT', 'ERROR') THEN 'SYSTEM'
    ELSE 'EXECUTOR'
END
WHERE agent_role IS NULL;

ALTER TABLE agent_steps
    DROP CONSTRAINT IF EXISTS ck_agent_steps_status;

ALTER TABLE agent_steps
    ADD CONSTRAINT ck_agent_steps_status
        CHECK (status IN ('PENDING', 'RUNNING', 'WAITING_APPROVAL', 'RETRYING', 'SUCCEEDED', 'FAILED', 'SKIPPED'));

CREATE INDEX idx_agent_steps_trace_id
    ON agent_steps(trace_id);

CREATE INDEX idx_agent_steps_agent_role
    ON agent_steps(agent_role);
