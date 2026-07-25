package com.jadhavr.erp.storage;

import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3ObjectStorageService implements ObjectStorageService {
    private final S3Client s3;
    private final String bucket;

    public S3ObjectStorageService(
            S3Client s3,
            @Value("${app.aws.private-upload-bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        String safeKey = ObjectKeyPolicy.requireSafe(key);
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(safeKey)
                            .contentType(contentType)
                            .contentDisposition("inline")
                            .serverSideEncryption(ServerSideEncryption.AES256)
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (RuntimeException exception) {
            throw new BadRequestException("Unable to store the uploaded file");
        }
    }

    @Override
    public StoredObject get(String key) {
        try {
            ResponseBytes<GetObjectResponse> response = s3.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(ObjectKeyPolicy.requireSafe(key))
                    .build());
            String contentType = response.response().contentType();
            return new StoredObject(response.asByteArray(), contentType == null
                    ? "application/octet-stream" : contentType);
        } catch (NoSuchKeyException exception) {
            throw new ResourceNotFoundException("Stored file not found");
        } catch (RuntimeException exception) {
            throw new ResourceNotFoundException("Stored file is temporarily unavailable");
        }
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(ObjectKeyPolicy.requireSafe(key))
                .build());
    }
}
