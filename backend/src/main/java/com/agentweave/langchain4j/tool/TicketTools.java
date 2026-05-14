package com.agentweave.langchain4j.tool;

import com.agentweave.shared.security.RequireToolPermission;
import com.agentweave.tool.ticket.TicketQueryRequest;
import com.agentweave.tool.ticket.TicketQueryResult;
import com.agentweave.tool.ticket.TicketQueryService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.validation.Valid;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
public class TicketTools {

    private final TicketQueryService ticketQueryService;

    public TicketTools(TicketQueryService ticketQueryService) {
        this.ticketQueryService = ticketQueryService;
    }

    @Tool(name = "query_ticket", value = "Query a single ticket summary by ticket number.")
    @RequireToolPermission("tool:ticket:query")
    public TicketQueryResult queryTicket(
            @P(value = "Ticket number in INC-00000 format.", required = true) @Valid TicketQueryRequest request) {
        return ticketQueryService.query(request);
    }
}
