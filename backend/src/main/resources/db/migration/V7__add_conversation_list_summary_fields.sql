ALTER TABLE conversations
    ADD COLUMN message_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_message_preview VARCHAR(200),
    ADD COLUMN last_message_at TIMESTAMP WITH TIME ZONE;

UPDATE conversations c
SET message_count = summary.message_count,
    last_message_preview = summary.last_message_preview,
    last_message_at = summary.last_message_at
FROM (
    SELECT
        m.conversation_id,
        COUNT(*) AS message_count,
        LEFT((ARRAY_AGG(REGEXP_REPLACE(TRIM(m.content), '\s+', ' ', 'g') ORDER BY m.created_at DESC))[1], 120) AS last_message_preview,
        MAX(m.created_at) AS last_message_at
    FROM conversation_messages m
    GROUP BY m.conversation_id
) summary
WHERE c.id = summary.conversation_id;

CREATE INDEX idx_conversations_owner_status_updated_at
    ON conversations(owner_user_id, status, updated_at DESC);

CREATE INDEX idx_conversations_owner_title
    ON conversations(owner_user_id, LOWER(title));
