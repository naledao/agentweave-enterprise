CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content TEXT,
    metadata JSONB,
    embedding vector(1024)
);

CREATE INDEX IF NOT EXISTS spring_ai_vector_index
    ON vector_store USING HNSW (embedding vector_cosine_ops);

CREATE INDEX IF NOT EXISTS idx_vector_store_metadata_gin
    ON vector_store USING GIN (metadata);

CREATE INDEX IF NOT EXISTS idx_vector_store_document_id
    ON vector_store((metadata ->> 'documentId'));

CREATE INDEX IF NOT EXISTS idx_vector_store_chunk_id
    ON vector_store((metadata ->> 'chunkId'));

CREATE INDEX IF NOT EXISTS idx_vector_store_business_domain
    ON vector_store((metadata ->> 'businessDomain'));

CREATE INDEX IF NOT EXISTS idx_vector_store_document_type
    ON vector_store((metadata ->> 'documentType'));

CREATE INDEX IF NOT EXISTS idx_vector_store_permission_level
    ON vector_store((metadata ->> 'permissionLevel'));
