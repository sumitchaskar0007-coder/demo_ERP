package com.collegeerp.erp.storage;

import com.collegeerp.erp.common.exception.BadRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecureFileContentValidatorTest {
    private final SecureFileContentValidator validator = new SecureFileContentValidator();

    @Test
    void parsesAndResavesPdfInsteadOfTrustingItsHeader() throws Exception {
        byte[] original = TestUploadFiles.pdf();
        byte[] marker = "<script>polyglot</script>".getBytes(StandardCharsets.US_ASCII);
        byte[] polyglot = Arrays.copyOf(original, original.length + marker.length);
        System.arraycopy(marker, 0, polyglot, original.length, marker.length);

        var result = validator.validateDocument(
                polyglot, MediaType.APPLICATION_PDF_VALUE, 100_000);

        assertEquals(MediaType.APPLICATION_PDF_VALUE, result.contentType());
        assertFalse(new String(result.content(), StandardCharsets.ISO_8859_1)
                .contains("<script>polyglot"));
        try (PDDocument parsed = Loader.loadPDF(result.content())) {
            assertEquals(1, parsed.getNumberOfPages());
        }
    }

    @Test
    void rejectsMalformedPdfEvenWhenHeaderLooksCorrect() {
        byte[] fake = "%PDF-1.7\n<script>alert(1)</script>"
                .getBytes(StandardCharsets.US_ASCII);

        assertThrows(BadRequestException.class, () -> validator.validateDocument(
                fake, MediaType.APPLICATION_PDF_VALUE, 100_000));
    }

    @Test
    void rejectsPdfWithActiveContent() throws Exception {
        byte[] activePdf;
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            COSDictionary action = new COSDictionary();
            action.setName(COSName.S, "JavaScript");
            action.setString(COSName.getPDFName("JS"), "app.alert('unsafe')");
            document.getDocumentCatalog().getCOSObject()
                    .setItem(COSName.getPDFName("OpenAction"), action);
            document.save(output);
            activePdf = output.toByteArray();
        }

        assertThrows(BadRequestException.class, () -> validator.validateDocument(
                activePdf, MediaType.APPLICATION_PDF_VALUE, 100_000));
    }

    @Test
    void rejectsImageWhenDecodedFormatDoesNotMatchDeclaredType() {
        MockMultipartFile disguised = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, TestUploadFiles.png());

        assertThrows(BadRequestException.class,
                () -> validator.validateImage(disguised, 100_000));
    }

    @Test
    void rejectsWebpByPolicyEvenIfAReaderIsAvailable() {
        MockMultipartFile webp = new MockMultipartFile(
                "file", "photo.webp", "image/webp", new byte[] {1, 2, 3, 4});

        BadRequestException exception = assertThrows(
                BadRequestException.class, () -> validator.validateImage(webp, 100_000));
        assertTrue(exception.getMessage().contains("JPEG or PNG"));
    }
}
