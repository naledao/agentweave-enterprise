UPDATE tool_definitions
SET input_schema = '{"type":"object","properties":{"serviceName":{"type":"string","description":"Service name to search, for example chat-service"},"keyword":{"type":"string","description":"Keyword, level, or traceId fragment to match"},"timeRange":{"type":"object","properties":{"from":{"type":"string","format":"date-time"},"to":{"type":"string","format":"date-time"}},"required":["from","to"]},"limit":{"type":"integer","minimum":1,"maximum":50,"description":"Maximum number of recent snippets to return"}},"required":["serviceName","keyword","timeRange"]}',
    output_schema = '{"type":"object","properties":{"summary":{"type":"string"},"hitCount":{"type":"integer"},"recentErrors":{"type":"array","items":{"type":"object","properties":{"timestamp":{"type":"string","format":"date-time"},"serviceName":{"type":"string"},"level":{"type":"string"},"message":{"type":"string"},"traceId":{"type":"string"}}}},"timeRange":{"type":"object","properties":{"from":{"type":"string","format":"date-time"},"to":{"type":"string","format":"date-time"}}}}}',
    risk_level = 'MEDIUM',
    updated_at = now()
WHERE code = 'log.search';
