UPDATE tool_definitions
SET input_schema = '{"type":"object","properties":{"ticketNo":{"type":"string","description":"Ticket number in INC-00000 format"}},"required":["ticketNo"]}',
    output_schema = '{"type":"object","properties":{"ticketNo":{"type":"string"},"title":{"type":"string"},"status":{"type":"string"},"priority":{"type":"string"},"assignee":{"type":"string"},"updatedAt":{"type":"string","format":"date-time"}}}',
    updated_at = now()
WHERE code = 'ticket.query';
