CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    content_length INTEGER NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(40) NOT NULL,
    error_message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_document_chunks_document_index UNIQUE (document_id, chunk_index),
    CONSTRAINT chk_document_chunks_index_non_negative CHECK (chunk_index >= 0),
    CONSTRAINT chk_document_chunks_content_not_blank CHECK (length(trim(content)) > 0),
    CONSTRAINT chk_document_chunks_content_length_positive CHECK (content_length > 0)
);

CREATE INDEX idx_document_chunks_document_id ON document_chunks(document_id);
CREATE INDEX idx_document_chunks_status ON document_chunks(status);
CREATE INDEX idx_document_chunks_metadata_gin ON document_chunks USING GIN (metadata);
CREATE INDEX idx_document_chunks_metadata_business_domain
    ON document_chunks((metadata ->> 'businessDomain'));
CREATE INDEX idx_document_chunks_metadata_document_type
    ON document_chunks((metadata ->> 'documentType'));
CREATE INDEX idx_document_chunks_metadata_permission_level
    ON document_chunks((metadata ->> 'permissionLevel'));
