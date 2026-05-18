ALTER TABLE tool_invocations
    ADD COLUMN tool_name VARCHAR(160),
    ADD COLUMN risk_level VARCHAR(40),
    ADD COLUMN workflow_run_id UUID,
    ADD COLUMN workflow_step_id UUID;

UPDATE tool_invocations inv
SET tool_name = def.name,
    risk_level = def.risk_level
FROM tool_definitions def
WHERE inv.tool_code = def.code;

UPDATE tool_invocations
SET tool_name = tool_code
WHERE tool_name IS NULL;

ALTER TABLE tool_invocations
    ALTER COLUMN tool_name SET NOT NULL,
    ADD CONSTRAINT ck_tool_invocations_risk_level
        CHECK (risk_level IS NULL OR risk_level IN ('LOW', 'MEDIUM', 'HIGH'));

CREATE INDEX idx_tool_invocations_workflow
    ON tool_invocations(workflow_run_id, workflow_step_id);
