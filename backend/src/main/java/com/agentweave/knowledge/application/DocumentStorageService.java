package com.agentweave.knowledge.application;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentStorageService {

    StoredDocumentObject store(
            String objectKey,
            InputStream inputStream,
            long size,
            String contentType,
            String checksum) throws IOException;

    InputStream read(String bucket, String objectKey) throws IOException;

    void delete(String bucket, String objectKey);
}
