package com.agentweave.tool.ticket;

import java.time.Instant;

public record TicketQueryResult(
        String ticketNo,
        String title,
        String status,
        String priority,
        String assignee,
        Instant updatedAt) {
}
