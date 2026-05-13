ALTER TABLE documents
    ADD COLUMN error_message VARCHAR(1000),
    ADD COLUMN trace_id VARCHAR(120);

CREATE INDEX idx_documents_trace_id ON documents(trace_id);
