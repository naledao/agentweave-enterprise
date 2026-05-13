package com.agentweave.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank @Size(max = 8000) String content,
        ResponseMode responseMode) {

    public ResponseMode normalizedResponseMode() {
        return responseMode == null ? ResponseMode.STREAM : responseMode;
    }
}
