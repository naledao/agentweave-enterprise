package com.agentweave.conversation.domain;

public enum ModelCallScenario {
    CHAT_SYNC,
    CHAT_STREAM,
    RAG_ANSWER,
    GRAPHRAG_EXTRACTION,
    PLANNER,
    EXECUTOR,
    REVIEWER
}
