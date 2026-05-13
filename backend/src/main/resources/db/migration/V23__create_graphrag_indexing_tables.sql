CREATE TABLE kg_entities (
    id UUID PRIMARY KEY,
    source_document_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    type VARCHAR(40) NOT NULL,
    description TEXT,
    aliases JSONB NOT NULL DEFAULT '[]'::jsonb,
    business_domain VARCHAR(120) NOT NULL,
    permission_level VARCHAR(80) NOT NULL,
    source_chunk_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE kg_entity_aliases (
    id UUID PRIMARY KEY,
    entity_id UUID NOT NULL,
    source_document_id UUID NOT NULL,
    alias VARCHAR(255) NOT NULL,
    normalized_alias VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_kg_entity_aliases_entity_normalized UNIQUE (entity_id, normalized_alias)
);

CREATE TABLE kg_chunk_entities (
    id UUID PRIMARY KEY,
    source_document_id UUID NOT NULL,
    chunk_id UUID NOT NULL,
    entity_id UUID NOT NULL,
    mention_count INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_kg_chunk_entities_chunk_entity UNIQUE (chunk_id, entity_id)
);

CREATE TABLE kg_relationships (
    id UUID PRIMARY KEY,
    source_document_id UUID NOT NULL,
    source_entity_id UUID NOT NULL,
    target_entity_id UUID NOT NULL,
    type VARCHAR(40) NOT NULL,
    description TEXT,
    confidence DOUBLE PRECISION NOT NULL,
    source_chunk_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_kg_relationships_document_pair UNIQUE (
        source_document_id,
        source_entity_id,
        target_entity_id,
        type
    )
);

CREATE TABLE graphrag_index_logs (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    trace_id VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    entity_count INTEGER NOT NULL DEFAULT 0,
    relationship_count INTEGER NOT NULL DEFAULT 0,
    chunk_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_kg_entities_document_id ON kg_entities(source_document_id);
CREATE INDEX idx_kg_entities_normalized_name ON kg_entities(normalized_name);
CREATE INDEX idx_kg_entities_type ON kg_entities(type);
CREATE INDEX idx_kg_entity_aliases_document_id ON kg_entity_aliases(source_document_id);
CREATE INDEX idx_kg_chunk_entities_document_id ON kg_chunk_entities(source_document_id);
CREATE INDEX idx_kg_chunk_entities_chunk_id ON kg_chunk_entities(chunk_id);
CREATE INDEX idx_kg_relationships_document_id ON kg_relationships(source_document_id);
CREATE INDEX idx_graphrag_index_logs_document_id ON graphrag_index_logs(document_id);
CREATE INDEX idx_graphrag_index_logs_trace_id ON graphrag_index_logs(trace_id);
CREATE INDEX idx_graphrag_index_logs_status ON graphrag_index_logs(status);
