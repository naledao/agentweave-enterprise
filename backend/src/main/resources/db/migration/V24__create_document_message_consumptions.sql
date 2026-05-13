CREATE TABLE document_message_consumptions (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    document_id UUID NOT NULL,
    trace_id VARCHAR(120) NOT NULL,
    consumer VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_document_message_consumptions_document_id
    ON document_message_consumptions(document_id);
CREATE INDEX idx_document_message_consumptions_event_type
    ON document_message_consumptions(event_type);
CREATE INDEX idx_document_message_consumptions_consumer
    ON document_message_consumptions(consumer);
