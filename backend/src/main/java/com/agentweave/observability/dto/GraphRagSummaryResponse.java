package com.agentweave.observability.dto;

public record GraphRagSummaryResponse(
        GraphRagIndexLogResponse latestIndexLog,
        GraphRagRetrievalLogResponse latestRetrievalLog,
        long indexLogCount,
        long retrievalLogCount) {
}
