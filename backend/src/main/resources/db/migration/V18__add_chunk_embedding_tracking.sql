ALTER TABLE document_chunks
    ADD COLUMN vector_id UUID,
    ADD COLUMN embedded_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN trace_id VARCHAR(120);

CREATE INDEX idx_document_chunks_vector_id ON document_chunks(vector_id);
CREATE INDEX idx_document_chunks_trace_id ON document_chunks(trace_id);
