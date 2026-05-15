-- Add agent workflow fields to model_call_logs table
ALTER TABLE model_call_logs
ADD COLUMN agent_stage VARCHAR(50),
ADD COLUMN agent_run_id UUID,
ADD COLUMN agent_step_id UUID;

-- Add indexes for agent fields
CREATE INDEX idx_model_call_logs_agent_run_id ON model_call_logs(agent_run_id);
CREATE INDEX idx_model_call_logs_agent_step_id ON model_call_logs(agent_step_id);
CREATE INDEX idx_model_call_logs_agent_stage ON model_call_logs(agent_stage);

-- Add foreign key constraints
ALTER TABLE model_call_logs
ADD CONSTRAINT fk_model_call_logs_agent_run_id
FOREIGN KEY (agent_run_id) REFERENCES agent_runs(id) ON DELETE SET NULL;

ALTER TABLE model_call_logs
ADD CONSTRAINT fk_model_call_logs_agent_step_id
FOREIGN KEY (agent_step_id) REFERENCES agent_steps(id) ON DELETE SET NULL;