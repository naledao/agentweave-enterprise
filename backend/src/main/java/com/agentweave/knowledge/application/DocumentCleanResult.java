package com.agentweave.knowledge.application;

public record DocumentCleanResult(
        String cleanedText,
        int originalLength,
        int textLength) {
}
