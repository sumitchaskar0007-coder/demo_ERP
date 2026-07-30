package com.jadhavr.erp.fee.service;

import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.security.TestSecurityUsers;
import com.jadhavr.erp.storage.ObjectStorageService;
import com.jadhavr.erp.user.entity.RoleName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProofStorageServiceTest {
    @Mock private ObjectStorageService storage;

    private PaymentProofStorageService service;

    @BeforeEach
    void setUp() {
        service = new PaymentProofStorageService(storage);
        SecurityContextHolder.getContext().setAuthentication(
                TestSecurityUsers.authentication(RoleName.STUDENT, 42L, 7L));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void savesPaymentProofUnderTenantScopedObjectKey() {
        byte[] pdf = "%PDF-1.7\nproof".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        MockMultipartFile file = new MockMultipartFile(
                "proof", "proof.pdf", MediaType.APPLICATION_PDF_VALUE, pdf);

        String key = service.save(file);

        assertTrue(key.matches(
                "^colleges/7/students/42/fee-payment-proofs/[0-9a-f-]+\\.pdf$"));
        ArgumentCaptor<byte[]> content = ArgumentCaptor.forClass(byte[].class);
        verify(storage).put(
                org.mockito.ArgumentMatchers.eq(key),
                content.capture(),
                org.mockito.ArgumentMatchers.eq(MediaType.APPLICATION_PDF_VALUE));
        assertArrayEquals(pdf, content.getValue());
    }

    @Test
    void infersContentTypeWhenBrowserSendsGenericBinaryType() {
        byte[] png = new byte[] {
                (byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3
        };
        MockMultipartFile file = new MockMultipartFile(
                "proof", "phone-upload.PNG", MediaType.APPLICATION_OCTET_STREAM_VALUE, png);

        String key = service.save(file);

        assertTrue(key.endsWith(".png"));
        verify(storage).put(key, png, MediaType.IMAGE_PNG_VALUE);
    }

    @Test
    void normalizesCommonJpegAlias() {
        byte[] jpeg = new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1, 2, 3};
        MockMultipartFile file = new MockMultipartFile(
                "proof", "camera.jpg", "image/jpg", jpeg);

        String key = service.save(file);

        assertTrue(key.endsWith(".jpg"));
        verify(storage).put(key, jpeg, MediaType.IMAGE_JPEG_VALUE);
    }

    @Test
    void rejectsContentWhoseSignatureDoesNotMatchItsType() {
        MockMultipartFile file = new MockMultipartFile(
                "proof", "proof.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[] {1, 2, 3});

        assertThrows(BadRequestException.class, () -> service.save(file));

        verify(storage, never()).put(anyString(), org.mockito.ArgumentMatchers.any(), anyString());
    }

    @Test
    void loadsMigratedObjectWithExtensionFallback() throws Exception {
        String key = "legacy-payment-proof.pdf";
        byte[] content = "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        when(storage.get(key)).thenReturn(new ObjectStorageService.StoredObject(
                content, MediaType.APPLICATION_OCTET_STREAM_VALUE));

        PaymentProofStorageService.PaymentProofResource result = service.load(key);

        assertEquals(MediaType.APPLICATION_PDF, result.mediaType());
        assertArrayEquals(content, result.resource().getInputStream().readAllBytes());
    }

    @Test
    void cleanupDoesNotMaskOriginalFailure() {
        doThrow(new IllegalStateException("temporary object-store failure"))
                .when(storage).delete("payment-proofs/key.pdf");

        service.delete("payment-proofs/key.pdf");

        verify(storage).delete("payment-proofs/key.pdf");
    }

    @Test
    void deletesGeneratedObjectWhenPutFails() {
        byte[] pdf = "%PDF-1.7\nproof".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        MockMultipartFile file = new MockMultipartFile(
                "proof", "proof.pdf", MediaType.APPLICATION_PDF_VALUE, pdf);
        doThrow(new BadRequestException("ambiguous object-store failure"))
                .when(storage).put(anyString(), any(byte[].class), eq(MediaType.APPLICATION_PDF_VALUE));
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);

        assertThrows(BadRequestException.class, () -> service.save(file));

        verify(storage).put(key.capture(), eq(pdf), eq(MediaType.APPLICATION_PDF_VALUE));
        verify(storage).delete(key.getValue());
    }
}
