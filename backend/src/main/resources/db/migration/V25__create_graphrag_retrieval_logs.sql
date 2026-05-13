CREATE TABLE graphrag_retrieval_logs (
    id UUID PRIMARY KEY,
    conversation_id UUID,
    message_id UUID,
    trace_id VARCHAR(120) NOT NULL,
    query TEXT NOT NULL,
    retrieval_mode VARCHAR(40) NOT NULL,
    business_domain VARCHAR(120),
    permission_level VARCHAR(80),
    document_id UUID,
    max_depth INTEGER NOT NULL,
    max_path_count INTEGER NOT NULL,
    resolved_entities JSONB NOT NULL DEFAULT '[]'::jsonb,
    matched_path_count INTEGER NOT NULL DEFAULT 0,
    filtered_path_count INTEGER NOT NULL DEFAULT 0,
    source_chunk_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    confidence_summary VARCHAR(1000),
    status VARCHAR(40) NOT NULL,
    error_message VARCHAR(1000),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_graphrag_retrieval_logs_conversation_id
    ON graphrag_retrieval_logs(conversation_id, created_at DESC);
CREATE INDEX idx_graphrag_retrieval_logs_message_id
    ON graphrag_retrieval_logs(message_id);
CREATE INDEX idx_graphrag_retrieval_logs_trace_id
    ON graphrag_retrieval_logs(trace_id);
CREATE INDEX idx_graphrag_retrieval_logs_document_id
    ON graphrag_retrieval_logs(document_id);
CREATE INDEX idx_graphrag_retrieval_logs_status
    ON graphrag_retrieval_logs(status);
CREATE INDEX idx_graphrag_retrieval_logs_retrieval_mode
    ON graphrag_retrieval_logs(retrieval_mode);
