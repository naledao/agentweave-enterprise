package com.agentweave.knowledge.application;

import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class DocumentParserService {

    private final DocumentParsingProperties properties;

    public DocumentParserService(DocumentParsingProperties properties) {
        this.properties = properties;
    }

    public DocumentParseResult parse(String filename, String contentType, InputStream inputStream) {
        String normalizedContentType = normalizeContentType(contentType);
        String extension = extension(filename);
        if (!properties.allowedContentTypes().contains(normalizedContentType)
                || !properties.allowedExtensions().contains(extension)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "unsupported document file type for parsing");
        }

        try {
            String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\uFEFF", "")
                    .trim();
            if (text.isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "parsed text must not be blank");
            }
            return new DocumentParseResult(text, "textLength=%d".formatted(text.length()));
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "failed to parse document object");
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String extension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
