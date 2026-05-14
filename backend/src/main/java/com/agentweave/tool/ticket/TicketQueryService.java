package com.agentweave.tool.ticket;

import com.agentweave.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketQueryService {

    private final TicketClient ticketClient;

    public TicketQueryService(TicketClient ticketClient) {
        this.ticketClient = ticketClient;
    }

    @Transactional(readOnly = true)
    public TicketQueryResult query(TicketQueryRequest request) {
        return ticketClient.findByTicketNo(request.normalizedTicketNo())
                .orElseThrow(() -> new ResourceNotFoundException("ticket not found"));
    }
}
