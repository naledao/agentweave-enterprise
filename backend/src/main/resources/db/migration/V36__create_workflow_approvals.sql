ALTER TABLE agent_runs
    DROP CONSTRAINT IF EXISTS ck_agent_runs_status;

UPDATE agent_runs
SET status = upper(status);

ALTER TABLE agent_runs
    ALTER COLUMN status SET DEFAULT 'CREATED';

ALTER TABLE agent_runs
    ADD CONSTRAINT ck_agent_runs_status
        CHECK (status IN ('CREATED', 'PLANNING', 'EXECUTING', 'WAITING_APPROVAL', 'REVIEWING', 'SUCCEEDED', 'FAILED', 'CANCELLED'));

ALTER TABLE agent_steps
    DROP CONSTRAINT IF EXISTS ck_agent_steps_status;

UPDATE agent_steps
SET status = upper(status),
    step_type = upper(step_type);

ALTER TABLE agent_steps
    ALTER COLUMN status SET DEFAULT 'PENDING';

ALTER TABLE agent_steps
    ADD CONSTRAINT ck_agent_steps_status
        CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'SKIPPED'));

ALTER TABLE agent_steps
    DROP CONSTRAINT IF EXISTS ck_agent_steps_step_type;

ALTER TABLE agent_steps
    ADD CONSTRAINT ck_agent_steps_step_type
        CHECK (step_type IN ('PLANNING', 'RAG_SEARCH', 'GRAPH_RAG_SEARCH', 'TOOL_CALL', 'REVIEW', 'FINAL_ANSWER', 'HUMAN_APPROVAL', 'CHECKPOINT', 'ERROR'));

CREATE TABLE agent_workflow_approvals (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL,
    step_id UUID NOT NULL,
    tool_code VARCHAR(120) NOT NULL,
    risk_level VARCHAR(40) NOT NULL,
    request_summary TEXT,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    requested_by UUID NOT NULL,
    approved_by UUID,
    decision_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at TIMESTAMPTZ,
    CONSTRAINT fk_agent_workflow_approvals_run_id
        FOREIGN KEY (run_id) REFERENCES agent_runs(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_workflow_approvals_step_id
        FOREIGN KEY (step_id) REFERENCES agent_steps(id) ON DELETE CASCADE,
    CONSTRAINT ck_agent_workflow_approvals_risk_level
        CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_agent_workflow_approvals_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

CREATE INDEX idx_agent_workflow_approvals_status_created_at
    ON agent_workflow_approvals(status, created_at DESC);

CREATE INDEX idx_agent_workflow_approvals_run_id
    ON agent_workflow_approvals(run_id);

CREATE INDEX idx_agent_workflow_approvals_requested_by_created_at
    ON agent_workflow_approvals(requested_by, created_at DESC);

INSERT INTO permissions (id, code, name, type, description) VALUES
('10000000-0000-0000-0000-000000000030', 'workflow:approval:read', 'Read workflow approvals', 'API', 'Read workflow approval records'),
('10000000-0000-0000-0000-000000000031', 'workflow:approval:write', 'Decide workflow approvals', 'API', 'Approve or reject high-risk workflow tool calls'),
('10000000-0000-0000-0000-000000000032', 'workflow:approval:self', 'Approve own workflow approvals', 'API', 'Approve high-risk workflow tool calls requested by the same user')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    type = EXCLUDED.type,
    description = EXCLUDED.description,
    updated_at = now();

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('workflow:approval:read', 'workflow:approval:write')
WHERE r.code IN ('ADMIN', 'OPERATOR')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'workflow:approval:self'
WHERE r.code = 'ADMIN'
ON CONFLICT DO NOTHING;
