package com.jadhavr.erp.storage;

import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3ObjectStorageService implements ObjectStorageService, PresignedObjectStorageService {
    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final boolean malwareScanRequired;

    public S3ObjectStorageService(
            S3Client s3,
            S3Presigner presigner,
            @Value("${app.aws.private-upload-bucket}") String bucket) {
        this(s3, presigner, bucket, false);
    }

    @Autowired
    public S3ObjectStorageService(
            S3Client s3,
            S3Presigner presigner,
            @Value("${app.aws.private-upload-bucket}") String bucket,
            @Value("${app.storage.malware-scan.required:true}") boolean malwareScanRequired) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
        this.malwareScanRequired = malwareScanRequired;
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        String safeKey = ObjectKeyPolicy.requireSafe(key);
        try {
            String disposition = contentType != null && contentType.startsWith("image/")
                    ? "inline" : "attachment";
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(safeKey)
                            .contentType(contentType)
                            .contentDisposition(disposition)
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

    @Override
    public PresignedUpload presignUpload(
            String key,
            String contentType,
            long contentLength,
            String checksumSha256,
            Duration expiry) {
        String safeKey = ObjectKeyPolicy.requireSafe(key);
        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(safeKey)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .checksumSHA256(checksumSha256)
                    .build();
            PresignedPutObjectRequest request = presigner.presignPutObject(
                    PutObjectPresignRequest.builder()
                            .signatureDuration(expiry)
                            .putObjectRequest(objectRequest)
                            .build());
            return new PresignedUpload(
                    request.url().toString(),
                    request.expiration(),
                    browserSettableHeaders(request.signedHeaders()));
        } catch (RuntimeException exception) {
            throw new BadRequestException("Unable to create the document upload URL");
        }
    }

    @Override
    public ObjectMetadata head(String key) {
        try {
            HeadObjectResponse response = s3.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(ObjectKeyPolicy.requireSafe(key))
                    .checksumMode(ChecksumMode.ENABLED)
                    .build());
            String scanStatus = malwareScanRequired ? malwareScanStatus(key) : null;
            return new ObjectMetadata(
                    response.contentLength() == null ? -1 : response.contentLength(),
                    response.contentType(),
                    response.checksumSHA256(),
                    scanStatus);
        } catch (NoSuchKeyException exception) {
            throw new ResourceNotFoundException("Uploaded document object was not found");
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new ResourceNotFoundException("Uploaded document object was not found");
            }
            throw new BadRequestException("Unable to verify the uploaded document");
        } catch (RuntimeException exception) {
            throw new BadRequestException("Unable to verify the uploaded document");
        }
    }

    @Override
    public PresignedDownload presignDownload(
            String key,
            String filename,
            String contentType,
            Duration expiry) {
        String safeKey = ObjectKeyPolicy.requireSafe(key);
        try {
            String disposition = ContentDisposition.attachment()
                    .filename(filename, StandardCharsets.UTF_8)
                    .build()
                    .toString();
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(safeKey)
                    .responseContentType(contentType)
                    .responseContentDisposition(disposition)
                    .build();
            PresignedGetObjectRequest request = presigner.presignGetObject(
                    GetObjectPresignRequest.builder()
                            .signatureDuration(expiry)
                            .getObjectRequest(objectRequest)
                            .build());
            return new PresignedDownload(request.url().toString(), request.expiration());
        } catch (RuntimeException exception) {
            throw new BadRequestException("Unable to create the document download URL");
        }
    }

    private String malwareScanStatus(String key) {
        var response = s3.getObjectTagging(GetObjectTaggingRequest.builder()
                .bucket(bucket)
                .key(ObjectKeyPolicy.requireSafe(key))
                .build());
        if (response == null || response.tagSet() == null) {
            return null;
        }
        return response.tagSet().stream()
                .filter(tag -> "GuardDutyMalwareScanStatus".equals(tag.key()))
                .map(software.amazon.awssdk.services.s3.model.Tag::value)
                .findFirst()
                .orElse(null);
    }

    private Map<String, List<String>> browserSettableHeaders(
            Map<String, List<String>> signedHeaders) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        signedHeaders.forEach((name, values) -> {
            if (!name.equalsIgnoreCase("host") && !name.equalsIgnoreCase("content-length")) {
                result.put(name, List.copyOf(values));
            }
        });
        return Map.copyOf(result);
    }
}
