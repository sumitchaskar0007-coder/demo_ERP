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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ObjectStorageServiceTest {
    @Mock private S3Client s3;

    @Test
    void storesPrivateEncryptedObjectUsingGeneratedSafeKey() {
        S3ObjectStorageService storage = new S3ObjectStorageService(s3, "private-uploads");
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        storage.put("colleges/7/admissions/9/photo/id.png", new byte[] {1, 2}, "image/png");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(request.capture(), any(RequestBody.class));
        assertEquals("private-uploads", request.getValue().bucket());
        assertEquals(ServerSideEncryption.AES256, request.getValue().serverSideEncryption());
        assertEquals("inline", request.getValue().contentDisposition());
    }

    @Test
    void loadsObjectWithoutCreatingPublicUrl() {
        S3ObjectStorageService storage = new S3ObjectStorageService(s3, "private-uploads");
        GetObjectResponse response = GetObjectResponse.builder().contentType("image/png").build();
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(response, new byte[] {4, 5, 6}));

        ObjectStorageService.StoredObject object = storage.get("colleges/7/branding/logo/id.png");

        assertArrayEquals(new byte[] {4, 5, 6}, object.content());
        assertEquals("image/png", object.contentType());
    }

    @Test
    void rejectsTraversalBeforeCallingS3() {
        S3ObjectStorageService storage = new S3ObjectStorageService(s3, "private-uploads");
        assertThrows(BadRequestException.class,
                () -> storage.put("colleges/7/../secret", new byte[] {1}, "image/png"));
    }
}
