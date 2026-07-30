package com.jadhavr.erp.storage;

import com.jadhavr.erp.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ObjectStorageServiceTest {
    @Mock private S3Client s3;
    @Mock private S3Presigner presigner;

    @Test
    void storesPrivateObjectUsingBucketDefaultEncryption() {
        S3ObjectStorageService storage = storage();
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        storage.put("colleges/7/admissions/9/photo/id.png", new byte[] {1, 2}, "image/png");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(request.capture(), any(RequestBody.class));
        assertEquals("private-uploads", request.getValue().bucket());
        assertNull(request.getValue().serverSideEncryption());
        assertEquals("inline", request.getValue().contentDisposition());
    }

    @Test
    void loadsObjectWithoutCreatingPublicUrl() {
        S3ObjectStorageService storage = storage();
        GetObjectResponse response = GetObjectResponse.builder().contentType("image/png").build();
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(response, new byte[] {4, 5, 6}));

        ObjectStorageService.StoredObject object = storage.get("colleges/7/branding/logo/id.png");

        assertArrayEquals(new byte[] {4, 5, 6}, object.content());
        assertEquals("image/png", object.contentType());
    }

    @Test
    void rejectsTraversalBeforeCallingS3() {
        S3ObjectStorageService storage = storage();
        assertThrows(BadRequestException.class,
                () -> storage.put("colleges/7/../secret", new byte[] {1}, "image/png"));
    }

    @Test
    void presignsChecksumBoundPrivateUpload() throws Exception {
        S3ObjectStorageService storage = storage();
        PresignedPutObjectRequest presigned = org.mockito.Mockito.mock(PresignedPutObjectRequest.class);
        Instant expiration = Instant.parse("2026-07-28T13:00:00Z");
        when(presigned.url()).thenReturn(URI.create("https://uploads.example.test/signed").toURL());
        when(presigned.expiration()).thenReturn(expiration);
        when(presigned.signedHeaders()).thenReturn(Map.of(
                "host", List.of("private-uploads.s3.amazonaws.com"),
                "content-length", List.of("2048"),
                "content-type", List.of("application/pdf"),
                "x-amz-checksum-sha256",
                List.of("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")));
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

        PresignedObjectStorageService.PresignedUpload result = storage.presignUpload(
                "colleges/7/admission-documents/18fd6a4d-11ba-4a24-8313-00cad87c3f47.pdf",
                "application/pdf",
                2048,
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                Duration.ofMinutes(5));

        ArgumentCaptor<PutObjectPresignRequest> request =
                ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(presigner).presignPutObject(request.capture());
        PutObjectRequest objectRequest = request.getValue().putObjectRequest();
        assertEquals("private-uploads", objectRequest.bucket());
        assertEquals(2048, objectRequest.contentLength());
        assertEquals("application/pdf", objectRequest.contentType());
        assertEquals(
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                objectRequest.checksumSHA256());
        assertNull(objectRequest.acl());
        assertNull(objectRequest.serverSideEncryption());
        assertEquals(expiration, result.expiresAt());
        assertFalse(result.requiredHeaders().isEmpty());
        assertFalse(result.requiredHeaders().containsKey("host"));
        assertFalse(result.requiredHeaders().containsKey("content-length"));
    }

    @Test
    void headsObjectWithChecksumModeEnabled() {
        S3ObjectStorageService storage = storage();
        when(s3.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder()
                .contentLength(2048L)
                .contentType("application/pdf")
                .checksumSHA256("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .build());

        PresignedObjectStorageService.ObjectMetadata result = storage.head(
                "colleges/7/admission-documents/18fd6a4d-11ba-4a24-8313-00cad87c3f47.pdf");

        ArgumentCaptor<HeadObjectRequest> request = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(s3).headObject(request.capture());
        assertEquals(ChecksumMode.ENABLED, request.getValue().checksumMode());
        assertEquals(2048, result.contentLength());
        assertEquals("application/pdf", result.contentType());
        assertEquals(
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                result.checksumSha256());
    }

    @Test
    void presignsShortLivedAttachmentDownload() throws Exception {
        S3ObjectStorageService storage = storage();
        PresignedGetObjectRequest presigned = org.mockito.Mockito.mock(PresignedGetObjectRequest.class);
        Instant expiration = Instant.parse("2026-07-28T13:00:00Z");
        when(presigned.url()).thenReturn(URI.create("https://downloads.example.test/signed").toURL());
        when(presigned.expiration()).thenReturn(expiration);
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        PresignedObjectStorageService.PresignedDownload result = storage.presignDownload(
                "colleges/7/admission-documents/18fd6a4d-11ba-4a24-8313-00cad87c3f47.pdf",
                "marksheet.pdf",
                "application/pdf",
                Duration.ofMinutes(2));

        ArgumentCaptor<GetObjectPresignRequest> request =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(presigner).presignGetObject(request.capture());
        assertEquals("application/pdf", request.getValue().getObjectRequest().responseContentType());
        assertTrue(request.getValue().getObjectRequest().responseContentDisposition()
                .startsWith("attachment"));
        assertEquals(expiration, result.expiresAt());
    }

    private S3ObjectStorageService storage() {
        return new S3ObjectStorageService(s3, presigner, "private-uploads");
    }
}
