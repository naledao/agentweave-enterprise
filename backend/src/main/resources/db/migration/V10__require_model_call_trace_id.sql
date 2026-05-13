UPDATE model_call_logs
SET trace_id = 'legacy-' || id::text
WHERE trace_id IS NULL
   OR btrim(trace_id) = '';

ALTER TABLE model_call_logs
    ALTER COLUMN trace_id SET NOT NULL;
