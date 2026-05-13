package com.agentweave.knowledge.application;

import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DocumentCleaningService {

    public DocumentCleanResult clean(String parsedText) {
        if (parsedText == null || parsedText.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "cleaned text must not be blank");
        }

        String normalizedText = normalizeLineBreaks(parsedText);
        List<String> paragraphs = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();

        for (String line : normalizedText.split("\n", -1)) {
            String cleanedLine = sanitizeLine(line);
            if (cleanedLine.isBlank()) {
                if (paragraph.length() > 0) {
                    paragraphs.add(paragraph.toString());
                    paragraph.setLength(0);
                }
                continue;
            }

            if (paragraph.length() > 0) {
                paragraph.append('\n');
            }
            paragraph.append(cleanedLine);
        }

        if (paragraph.length() > 0) {
            paragraphs.add(paragraph.toString());
        }

        if (paragraphs.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "cleaned text must not be blank");
        }

        String cleanedText = String.join("\n\n", paragraphs);
        return new DocumentCleanResult(cleanedText, parsedText.length(), cleanedText.length());
    }

    private String normalizeLineBreaks(String text) {
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\u2028', '\n')
                .replace('\u2029', '\n')
                .replace("\uFEFF", "");
    }

    private String sanitizeLine(String line) {
        StringBuilder builder = new StringBuilder(line.length());
        boolean previousSpace = false;

        for (int index = 0; index < line.length(); ) {
            int codePoint = line.codePointAt(index);
            index += Character.charCount(codePoint);

            if (codePoint == '\uFEFF'
                    || Character.isISOControl(codePoint)
                    || Character.getType(codePoint) == Character.FORMAT
                    || isWhitespaceLike(codePoint)) {
                if (!previousSpace && builder.length() > 0) {
                    builder.append(' ');
                    previousSpace = true;
                }
                continue;
            }

            builder.appendCodePoint(codePoint);
            previousSpace = false;
        }

        int start = 0;
        int end = builder.length();
        while (start < end && builder.charAt(start) == ' ') {
            start++;
        }
        while (end > start && builder.charAt(end - 1) == ' ') {
            end--;
        }
        return builder.substring(start, end);
    }

    private boolean isWhitespaceLike(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }
}
