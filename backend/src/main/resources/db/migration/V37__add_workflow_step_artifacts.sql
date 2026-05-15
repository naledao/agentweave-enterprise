ALTER TABLE agent_steps
    ADD COLUMN citations JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN graph_paths JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN tool_calls JSONB NOT NULL DEFAULT '[]'::jsonb;

CREATE INDEX idx_agent_steps_citations_gin
    ON agent_steps USING GIN (citations);

CREATE INDEX idx_agent_steps_graph_paths_gin
    ON agent_steps USING GIN (graph_paths);

CREATE INDEX idx_agent_steps_tool_calls_gin
    ON agent_steps USING GIN (tool_calls);
