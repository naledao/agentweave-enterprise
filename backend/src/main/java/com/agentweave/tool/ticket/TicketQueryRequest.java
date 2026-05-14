package com.agentweave.tool.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TicketQueryRequest(
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^INC-\\d{5}$", message = "must match INC-00000 format")
        String ticketNo) {

    public String normalizedTicketNo() {
        return ticketNo == null ? null : ticketNo.trim().toUpperCase();
    }
}
