ALTER TABLE graphrag_index_logs
    ADD COLUMN chunk_entity_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN neo4j_enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN duration_ms BIGINT NOT NULL DEFAULT 0;

UPDATE graphrag_index_logs
SET duration_ms = GREATEST(
        0,
        FLOOR(EXTRACT(EPOCH FROM (completed_at - started_at)) * 1000)::BIGINT
    )
WHERE completed_at IS NOT NULL;

ALTER TABLE graphrag_retrieval_logs
    ADD COLUMN duration_ms BIGINT NOT NULL DEFAULT 0;

UPDATE graphrag_retrieval_logs
SET duration_ms = GREATEST(
        0,
        FLOOR(EXTRACT(EPOCH FROM (completed_at - started_at)) * 1000)::BIGINT
    )
WHERE completed_at IS NOT NULL;

UPDATE graphrag_retrieval_logs
SET status = 'SUCCESS'
WHERE status = 'COMPLETED';
