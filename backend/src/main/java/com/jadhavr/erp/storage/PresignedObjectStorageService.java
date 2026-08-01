package com.jadhavr.erp.storage;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * S3-only operations used by direct browser transfers. Local storage deliberately
 * does not implement this contract, so existing multipart endpoints remain the
 * local-development fallback.
 */
public interface PresignedObjectStorageService {

    PresignedUpload presignUpload(
            String key,
            String contentType,
            long contentLength,
            String checksumSha256,
            Duration expiry);

    ObjectMetadata head(String key);

    PresignedDownload presignDownload(
            String key,
            String filename,
            String contentType,
            Duration expiry);

    record PresignedUpload(
            String url,
            Instant expiresAt,
            Map<String, List<String>> requiredHeaders) {
        public PresignedUpload {
            requiredHeaders = Map.copyOf(requiredHeaders);
        }
    }

    record ObjectMetadata(
            long contentLength,
            String contentType,
            String checksumSha256,
            String malwareScanStatus) {
        public ObjectMetadata(long contentLength, String contentType, String checksumSha256) {
            this(contentLength, contentType, checksumSha256, null);
        }
    }

    record PresignedDownload(String url, Instant expiresAt) {}
}
