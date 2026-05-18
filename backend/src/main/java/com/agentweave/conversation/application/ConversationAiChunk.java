package com.agentweave.conversation.application;

public record ConversationAiChunk(
        String content,
        ConversationAiResponse metadata) {

    public ConversationAiChunk {
        content = content == null ? "" : content;
    }

    public static ConversationAiChunk content(String content) {
        return new ConversationAiChunk(content, null);
    }

    public static ConversationAiChunk metadata(ConversationAiResponse metadata) {
        return new ConversationAiChunk("", metadata);
    }

    public boolean hasContent() {
        return !content.isBlank();
    }

    public boolean hasMetadata() {
        return metadata != null;
    }
}
