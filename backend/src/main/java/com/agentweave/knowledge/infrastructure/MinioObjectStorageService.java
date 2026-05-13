package com.agentweave.knowledge.infrastructure;

import com.agentweave.knowledge.application.DocumentStorageException;
import com.agentweave.knowledge.application.DocumentStorageService;
import com.agentweave.knowledge.application.KnowledgeStorageProperties;
import com.agentweave.knowledge.application.StoredDocumentObject;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MinioObjectStorageService implements DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioObjectStorageService.class);
    private static final long PART_SIZE = 10L * 1024L * 1024L;

    private final MinioClient minioClient;
    private final KnowledgeStorageProperties properties;

    public MinioObjectStorageService(MinioClient minioClient, KnowledgeStorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public StoredDocumentObject store(
            String objectKey,
            InputStream inputStream,
            long size,
            String contentType,
            String checksum) throws IOException {
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(inputStream, size, PART_SIZE)
                    .contentType(contentType)
                    .build());
            return new StoredDocumentObject(properties.bucket(), objectKey, checksum, size);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DocumentStorageException("failed to store document object", ex);
        }
    }

    @Override
    public InputStream read(String bucket, String objectKey) throws IOException {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DocumentStorageException("failed to read document object", ex);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to delete document object: bucket={}, objectKey={}", bucket, objectKey, ex);
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.bucket())
                .build());
        if (!exists && properties.createBucketIfMissing()) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.bucket())
                    .build());
        }
        if (!exists && !properties.createBucketIfMissing()) {
            throw new DocumentStorageException("document storage bucket does not exist", null);
        }
    }
}
