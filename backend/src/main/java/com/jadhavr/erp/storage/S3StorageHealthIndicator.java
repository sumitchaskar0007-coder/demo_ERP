package com.jadhavr.erp.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@Component("storage")
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3StorageHealthIndicator implements HealthIndicator {
    private final S3Client s3;
    private final String bucket;

    public S3StorageHealthIndicator(
            S3Client s3,
            @Value("${app.aws.private-upload-bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public Health health() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return Health.up().build();
        } catch (RuntimeException exception) {
            return Health.down().withDetail("reason", "Private object storage is unavailable").build();
        }
    }
}
