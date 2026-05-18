ALTER TABLE model_call_logs
    ALTER COLUMN conversation_id DROP NOT NULL,
    ALTER COLUMN message_id DROP NOT NULL;
