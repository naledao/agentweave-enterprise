package com.agentweave.tool.ticket;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TicketClient {

    private final Map<String, TicketQueryResult> demoTickets = Map.of(
            "INC-10001",
            new TicketQueryResult(
                    "INC-10001",
                    "Knowledge ingestion pipeline retries are backing up",
                    "OPEN",
                    "P1",
                    "Liu Wei",
                    Instant.parse("2026-05-13T09:30:00Z")),
            "INC-10002",
            new TicketQueryResult(
                    "INC-10002",
                    "Chat SSE stream disconnects during long tool runs",
                    "IN_PROGRESS",
                    "P2",
                    "Chen Yu",
                    Instant.parse("2026-05-13T15:45:00Z")),
            "INC-10003",
            new TicketQueryResult(
                    "INC-10003",
                    "Vector search returns stale document citations",
                    "RESOLVED",
                    "P3",
                    "Zhang Min",
                    Instant.parse("2026-05-12T11:20:00Z")));

    public Optional<TicketQueryResult> findByTicketNo(String ticketNo) {
        if (ticketNo == null || ticketNo.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(demoTickets.get(ticketNo.trim().toUpperCase()));
    }
}
