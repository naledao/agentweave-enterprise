package com.agentweave.knowledge.application;

import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DocumentChunkingService {

    private final DocumentChunkingProperties properties;

    public DocumentChunkingService(DocumentChunkingProperties properties) {
        this.properties = properties;
    }

    public List<String> split(String cleanedText) {
        if (cleanedText == null || cleanedText.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "cleaned text must not be blank");
        }
        String text = cleanedText.trim();
        int chunkSize = properties.chunkSize();
        int overlapSize = properties.overlapSize();
        int step = chunkSize - overlapSize;

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            int adjustedEnd = adjustEnd(text, start, end);
            String chunk = text.substring(start, adjustedEnd).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (adjustedEnd >= text.length()) {
                break;
            }
            start = Math.max(adjustedEnd - overlapSize, start + 1);
        }
        return chunks;
    }

    private int adjustEnd(String text, int start, int end) {
        if (end >= text.length()) {
            return text.length();
        }
        int searchFloor = start + Math.max(properties.chunkSize() / 2, 1);
        int paragraphBreak = text.lastIndexOf("\n\n", end);
        if (paragraphBreak >= searchFloor) {
            return paragraphBreak;
        }
        int lineBreak = text.lastIndexOf('\n', end);
        if (lineBreak >= searchFloor) {
            return lineBreak;
        }
        int sentenceBreak = lastSentenceBreak(text, searchFloor, end);
        if (sentenceBreak >= searchFloor) {
            return sentenceBreak + 1;
        }
        int whitespace = lastWhitespace(text, searchFloor, end);
        if (whitespace >= searchFloor) {
            return whitespace;
        }
        return end;
    }

    private int lastSentenceBreak(String text, int searchFloor, int end) {
        int result = -1;
        for (int index = end - 1; index >= searchFloor; index--) {
            char value = text.charAt(index);
            if (value == '.' || value == '!' || value == '?' || value == '。' || value == '！' || value == '？') {
                result = index;
                break;
            }
        }
        return result;
    }

    private int lastWhitespace(String text, int searchFloor, int end) {
        for (int index = end - 1; index >= searchFloor; index--) {
            if (Character.isWhitespace(text.charAt(index))) {
                return index;
            }
        }
        return -1;
    }
}
