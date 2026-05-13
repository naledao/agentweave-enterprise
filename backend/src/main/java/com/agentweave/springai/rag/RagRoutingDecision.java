package com.agentweave.springai.rag;

public record RagRoutingDecision(
        RagRetrievalMode retrievalMode,
        RagRetrievalPlan retrievalPlan,
        String routingReason) {
}
