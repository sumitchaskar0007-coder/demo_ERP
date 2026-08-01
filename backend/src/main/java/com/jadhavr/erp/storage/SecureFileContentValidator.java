package com.jadhavr.erp.storage;

import com.jadhavr.erp.common.exception.BadRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class SecureFileContentValidator {
    private static final int MAX_DIMENSION = 4096;
    private static final long MAX_PIXELS = 16_000_000L;
    private static final int MAX_PDF_PAGES = 100;
    private static final int MAX_PDF_OBJECTS = 100_000;
    private static final Map<String, String> EXTENSIONS = Map.of(
            MediaType.APPLICATION_PDF_VALUE, ".pdf",
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png");
    private static final Set<COSName> FORBIDDEN_PDF_KEYS = Set.of(
            COSName.getPDFName("AA"),
            COSName.getPDFName("OpenAction"),
            COSName.getPDFName("JavaScript"),
            COSName.getPDFName("JS"),
            COSName.getPDFName("Launch"),
            COSName.getPDFName("EmbeddedFiles"),
            COSName.getPDFName("EF"),
            COSName.getPDFName("RichMedia"),
            COSName.getPDFName("XFA"),
            COSName.getPDFName("SubmitForm"),
            COSName.getPDFName("ImportData"),
            COSName.getPDFName("GoToE"));
    private static final Set<String> FORBIDDEN_PDF_ACTIONS = Set.of(
            "JavaScript", "Launch", "RichMedia", "SubmitForm", "ImportData", "GoToE");

    public ValidatedContent validateImage(MultipartFile file, long maximumBytes) {
        byte[] bytes = read(file, maximumBytes, "image");
        String declared = normalizeDeclaredType(file.getContentType());
        if (!MediaType.IMAGE_JPEG_VALUE.equals(declared)
                && !MediaType.IMAGE_PNG_VALUE.equals(declared)) {
            throw new BadRequestException("Only JPEG or PNG images are allowed");
        }
        return sanitizeImage(bytes, declared, maximumBytes);
    }

    public ValidatedContent validateDocument(MultipartFile file, long maximumBytes) {
        byte[] bytes = read(file, maximumBytes, "document");
        String declared = resolveDocumentType(file);
        return validateDocument(bytes, declared, maximumBytes);
    }

    public ValidatedContent validateDocument(
            byte[] bytes, String declaredContentType, long maximumBytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("Document must not be empty");
        }
        if (bytes.length > maximumBytes) {
            throw new BadRequestException("Document exceeds the size limit");
        }
        String declared = normalizeDeclaredType(declaredContentType);
        return switch (declared) {
            case MediaType.APPLICATION_PDF_VALUE -> sanitizePdf(bytes, maximumBytes);
            case MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE ->
                    sanitizeImage(bytes, declared, maximumBytes);
            default -> throw new BadRequestException(
                    "Only PDF, JPEG, or PNG documents are allowed");
        };
    }

    private ValidatedContent sanitizeImage(byte[] bytes, String declared, long maximumBytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(
                new ByteArrayInputStream(bytes))) {
            if (input == null) throw invalidImage();
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalidImage();
            ImageReader reader = readers.next();
            try {
                // Multi-image detection needs random access; seek-forward-only readers
                // can reject getNumImages(true) even for otherwise valid images.
                reader.setInput(input, false, true);
                String actual = imageContentType(reader.getFormatName());
                if (!declared.equals(actual)) {
                    throw new BadRequestException(
                            "Uploaded file content does not match its file type");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_DIMENSION
                        || height > MAX_DIMENSION || (long) width * height > MAX_PIXELS
                        || reader.getNumImages(true) != 1) {
                    throw new BadRequestException(
                            "Image dimensions must be at most 4096 x 4096 pixels");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) throw invalidImage();
                BufferedImage safeImage = MediaType.IMAGE_JPEG_VALUE.equals(actual)
                        ? toRgb(decoded) : decoded;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                String format = MediaType.IMAGE_JPEG_VALUE.equals(actual) ? "jpeg" : "png";
                if (!ImageIO.write(safeImage, format, output)) throw invalidImage();
                return result(output.toByteArray(), actual, maximumBytes);
            } finally {
                reader.dispose();
            }
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw invalidImage();
        }
    }

    private ValidatedContent sanitizePdf(byte[] bytes, long maximumBytes) {
        if (bytes.length < 8 || bytes[0] != '%' || bytes[1] != 'P'
                || bytes[2] != 'D' || bytes[3] != 'F' || bytes[4] != '-') {
            throw new BadRequestException("Uploaded file content is not a valid PDF");
        }
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new BadRequestException("Encrypted PDF documents are not allowed");
            }
            int pages = document.getNumberOfPages();
            if (pages < 1 || pages > MAX_PDF_PAGES) {
                throw new BadRequestException("PDF documents must contain 1 to 100 pages");
            }
            Set<COSBase> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            inspectPdfObject(document.getDocument().getTrailer(), visited);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.setAllSecurityToBeRemoved(true);
            document.save(output);
            return result(output.toByteArray(), MediaType.APPLICATION_PDF_VALUE, maximumBytes);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new BadRequestException("Uploaded file content is not a valid PDF");
        }
    }

    private void inspectPdfObject(COSBase base, Set<COSBase> visited) {
        if (base == null || !visited.add(base)) return;
        if (visited.size() > MAX_PDF_OBJECTS) {
            throw new BadRequestException("PDF document is too complex");
        }
        if (base instanceof COSObject object) {
            inspectPdfObject(object.getObject(), visited);
            return;
        }
        if (base instanceof COSArray array) {
            for (COSBase value : array) inspectPdfObject(value, visited);
            return;
        }
        if (!(base instanceof COSDictionary dictionary)) return;
        for (COSName key : dictionary.keySet()) {
            if (FORBIDDEN_PDF_KEYS.contains(key)) {
                throw new BadRequestException("PDF contains active or embedded content");
            }
            COSBase value = dictionary.getItem(key);
            if (value instanceof COSName name && FORBIDDEN_PDF_ACTIONS.contains(name.getName())) {
                throw new BadRequestException("PDF contains an unsafe action");
            }
            inspectPdfObject(value, visited);
        }
    }

    private byte[] read(MultipartFile file, long maximumBytes, String label) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a " + label);
        }
        if (maximumBytes < 1 || file.getSize() > maximumBytes) {
            throw new BadRequestException(
                    Character.toUpperCase(label.charAt(0)) + label.substring(1)
                            + " exceeds the size limit");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0 || bytes.length > maximumBytes) {
                throw new BadRequestException("Uploaded " + label + " has an invalid size");
            }
            return bytes;
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read the uploaded " + label);
        }
    }

    private String resolveDocumentType(MultipartFile file) {
        String declared = normalizeDeclaredType(file.getContentType());
        if ("image/jpg".equals(declared)) return MediaType.IMAGE_JPEG_VALUE;
        if (EXTENSIONS.containsKey(declared)) return declared;
        if (!declared.isBlank()
                && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(declared)) {
            throw new BadRequestException("Only PDF, JPEG, or PNG documents are allowed");
        }
        String filename = file.getOriginalFilename();
        String lowercase = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lowercase.endsWith(".pdf")) return MediaType.APPLICATION_PDF_VALUE;
        if (lowercase.endsWith(".jpg") || lowercase.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (lowercase.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        throw new BadRequestException("Only PDF, JPEG, or PNG documents are allowed");
    }

    private ValidatedContent result(byte[] sanitized, String contentType, long maximumBytes) {
        if (sanitized.length == 0 || sanitized.length > maximumBytes) {
            throw new BadRequestException("Sanitized file exceeds the size limit");
        }
        try {
            String checksum = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(sanitized));
            return new ValidatedContent(
                    sanitized, contentType, EXTENSIONS.get(contentType), checksum);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to checksum validated upload", exception);
        }
    }

    private String normalizeDeclaredType(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String imageContentType(String format) {
        String normalized = format.toLowerCase(Locale.ROOT);
        if ("jpeg".equals(normalized) || "jpg".equals(normalized)) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if ("png".equals(normalized)) return MediaType.IMAGE_PNG_VALUE;
        throw new BadRequestException("Only JPEG or PNG images are allowed");
    }

    private BufferedImage toRgb(BufferedImage source) {
        BufferedImage result = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, java.awt.Color.WHITE, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private BadRequestException invalidImage() {
        return new BadRequestException("Uploaded file content is not a valid image");
    }

    public record ValidatedContent(
            byte[] content, String contentType, String extension, String sha256) {
    }
}
