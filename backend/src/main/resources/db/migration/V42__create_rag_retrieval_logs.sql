CREATE TABLE rag_retrieval_logs (
    id UUID PRIMARY KEY,
    conversation_id UUID,
    message_id UUID,
    trace_id VARCHAR(120) NOT NULL,
    query TEXT NOT NULL,
    retrieval_mode VARCHAR(40) NOT NULL,
    metadata_filter JSONB NOT NULL DEFAULT '{}'::jsonb,
    business_domain VARCHAR(120),
    document_type VARCHAR(120),
    permission_level VARCHAR(80),
    time_range VARCHAR(160),
    document_id UUID,
    top_k INTEGER NOT NULL,
    similarity_threshold DOUBLE PRECISION NOT NULL,
    matched_chunk_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    citation_summaries JSONB NOT NULL DEFAULT '[]'::jsonb,
    score_summary VARCHAR(1000),
    citation_count INTEGER NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(40) NOT NULL,
    error_message VARCHAR(1000),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_rag_retrieval_logs_conversation_id
    ON rag_retrieval_logs(conversation_id, created_at DESC);
CREATE INDEX idx_rag_retrieval_logs_message_id
    ON rag_retrieval_logs(message_id, created_at DESC);
CREATE INDEX idx_rag_retrieval_logs_trace_id
    ON rag_retrieval_logs(trace_id);
CREATE INDEX idx_rag_retrieval_logs_document_id
    ON rag_retrieval_logs(document_id);
CREATE INDEX idx_rag_retrieval_logs_status
    ON rag_retrieval_logs(status);
CREATE INDEX idx_rag_retrieval_logs_retrieval_mode
    ON rag_retrieval_logs(retrieval_mode);
CREATE INDEX idx_rag_retrieval_logs_filter_domain
    ON rag_retrieval_logs(business_domain, document_type, permission_level);
