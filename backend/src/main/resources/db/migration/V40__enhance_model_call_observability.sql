ALTER TABLE model_call_logs
    ADD COLUMN model_name VARCHAR(160),
    ADD COLUMN scenario VARCHAR(60),
    ADD COLUMN prompt_summary TEXT,
    ADD COLUMN response_summary TEXT,
    ADD COLUMN input_tokens INTEGER,
    ADD COLUMN output_tokens INTEGER,
    ADD COLUMN total_tokens INTEGER,
    ADD COLUMN duration_ms BIGINT,
    ADD COLUMN workflow_run_id UUID,
    ADD COLUMN workflow_step_id UUID;

UPDATE model_call_logs
SET model_name = COALESCE(model_name, model),
    scenario = COALESCE(scenario, UPPER(agent_stage), 'CHAT_SYNC'),
    input_tokens = COALESCE(input_tokens, prompt_tokens),
    output_tokens = COALESCE(output_tokens, completion_tokens),
    total_tokens = COALESCE(total_tokens, COALESCE(prompt_tokens, 0) + COALESCE(completion_tokens, 0)),
    duration_ms = COALESCE(duration_ms, latency_ms),
    workflow_run_id = COALESCE(workflow_run_id, agent_run_id),
    workflow_step_id = COALESCE(workflow_step_id, agent_step_id);

UPDATE model_call_logs
SET scenario = CASE scenario
    WHEN 'PLANNING' THEN 'PLANNER'
    WHEN 'EXECUTION' THEN 'EXECUTOR'
    WHEN 'REVIEW' THEN 'REVIEWER'
    ELSE scenario
END;

UPDATE model_call_logs
SET status = 'SUCCESS'
WHERE status = 'SUCCEEDED';

ALTER TABLE model_call_logs
    ALTER COLUMN model_name SET NOT NULL,
    ALTER COLUMN scenario SET NOT NULL,
    ALTER COLUMN duration_ms SET NOT NULL,
    ALTER COLUMN trace_id SET NOT NULL,
    ALTER COLUMN model DROP NOT NULL,
    ALTER COLUMN latency_ms DROP NOT NULL;

CREATE INDEX idx_model_call_logs_trace_id
    ON model_call_logs(trace_id);

CREATE INDEX idx_model_call_logs_scenario_status_created_at
    ON model_call_logs(scenario, status, created_at DESC);

CREATE INDEX idx_model_call_logs_model_name_created_at
    ON model_call_logs(model_name, created_at DESC);

CREATE INDEX idx_model_call_logs_workflow_run_id
    ON model_call_logs(workflow_run_id);

CREATE INDEX idx_model_call_logs_workflow_step_id
    ON model_call_logs(workflow_step_id);

ALTER TABLE model_call_logs
    ADD CONSTRAINT fk_model_call_logs_workflow_run_id
    FOREIGN KEY (workflow_run_id) REFERENCES agent_runs(id) ON DELETE SET NULL;

ALTER TABLE model_call_logs
    ADD CONSTRAINT fk_model_call_logs_workflow_step_id
    FOREIGN KEY (workflow_step_id) REFERENCES agent_steps(id) ON DELETE SET NULL;
