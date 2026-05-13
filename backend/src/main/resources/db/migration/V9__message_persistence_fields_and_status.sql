ALTER TABLE conversation_messages
    ADD COLUMN user_id UUID,
    ADD COLUMN error_code VARCHAR(120),
    ADD COLUMN error_message VARCHAR(500),
    ADD COLUMN metadata TEXT NOT NULL DEFAULT '{}';

UPDATE conversation_messages m
SET user_id = c.owner_user_id
FROM conversations c
WHERE m.conversation_id = c.id
  AND m.user_id IS NULL;

ALTER TABLE conversation_messages
    ALTER COLUMN user_id SET NOT NULL;

UPDATE conversation_messages
SET status = 'SUCCEEDED'
WHERE status = 'COMPLETED';

ALTER TABLE model_call_logs
    ADD COLUMN error_code VARCHAR(120);
