ALTER TABLE rag_retrieval_logs
    ADD COLUMN workflow_run_id UUID,
    ADD COLUMN workflow_step_id UUID;

ALTER TABLE rag_retrieval_logs
    ADD CONSTRAINT fk_rag_retrieval_logs_workflow_run_id
        FOREIGN KEY (workflow_run_id) REFERENCES agent_runs(id) ON DELETE SET NULL;

ALTER TABLE rag_retrieval_logs
    ADD CONSTRAINT fk_rag_retrieval_logs_workflow_step_id
        FOREIGN KEY (workflow_step_id) REFERENCES agent_steps(id) ON DELETE SET NULL;

CREATE INDEX idx_rag_retrieval_logs_workflow_run_id
    ON rag_retrieval_logs(workflow_run_id);

CREATE INDEX idx_rag_retrieval_logs_workflow_step_id
    ON rag_retrieval_logs(workflow_step_id);

ALTER TABLE graphrag_retrieval_logs
    ADD COLUMN workflow_run_id UUID,
    ADD COLUMN workflow_step_id UUID;

ALTER TABLE graphrag_retrieval_logs
    ADD CONSTRAINT fk_graphrag_retrieval_logs_workflow_run_id
        FOREIGN KEY (workflow_run_id) REFERENCES agent_runs(id) ON DELETE SET NULL;

ALTER TABLE graphrag_retrieval_logs
    ADD CONSTRAINT fk_graphrag_retrieval_logs_workflow_step_id
        FOREIGN KEY (workflow_step_id) REFERENCES agent_steps(id) ON DELETE SET NULL;

CREATE INDEX idx_graphrag_retrieval_logs_workflow_run_id
    ON graphrag_retrieval_logs(workflow_run_id);

CREATE INDEX idx_graphrag_retrieval_logs_workflow_step_id
    ON graphrag_retrieval_logs(workflow_step_id);
