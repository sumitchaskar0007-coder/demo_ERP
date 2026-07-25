package com.jadhavr.erp.fee.service;

import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentProofStorageService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            MediaType.APPLICATION_PDF_VALUE, ".pdf",
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png",
            "image/webp", ".webp");
    private final Path root;

    public PaymentProofStorageService(
            @Value("${app.storage.payment-proof-dir:uploads/payment-proofs}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Payment proof file is required");
        if (file.getSize() > MAX_BYTES) throw new BadRequestException("Payment proof must not exceed 5 MB");
        String contentType = file.getContentType();
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new BadRequestException("Only PDF, JPEG, PNG, or WebP payment proofs are allowed");
        }
        verifySignature(file, contentType);
        String storageName = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(root);
            Files.copy(file.getInputStream(), safePath(storageName), StandardCopyOption.REPLACE_EXISTING);
            return storageName;
        } catch (IOException exception) {
            throw new BadRequestException("Unable to store the payment proof");
        }
    }

    public PaymentProofResource load(String storageName) {
        try {
            Path path = safePath(storageName);
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Payment proof file not found");
            }
            String mediaType = switch (extension(storageName)) {
                case ".pdf" -> MediaType.APPLICATION_PDF_VALUE;
                case ".jpg" -> MediaType.IMAGE_JPEG_VALUE;
                case ".png" -> MediaType.IMAGE_PNG_VALUE;
                case ".webp" -> "image/webp";
                default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
            };
            return new PaymentProofResource(resource, MediaType.parseMediaType(mediaType));
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Payment proof file not found");
        }
    }

    public void delete(String storageName) {
        if (storageName == null || storageName.isBlank()) return;
        try {
            Files.deleteIfExists(safePath(storageName));
        } catch (IOException ignored) {
            // Best-effort cleanup after a rejected database operation.
        }
    }

    private void verifySignature(MultipartFile file, String contentType) {
        try {
            byte[] header = file.getInputStream().readNBytes(12);
            boolean valid = switch (contentType) {
                case MediaType.APPLICATION_PDF_VALUE -> header.length >= 5
                        && header[0] == '%' && header[1] == 'P' && header[2] == 'D'
                        && header[3] == 'F' && header[4] == '-';
                case MediaType.IMAGE_JPEG_VALUE -> header.length >= 3
                        && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8
                        && (header[2] & 0xff) == 0xff;
                case MediaType.IMAGE_PNG_VALUE -> header.length >= 8
                        && (header[0] & 0xff) == 0x89 && header[1] == 'P'
                        && header[2] == 'N' && header[3] == 'G';
                case "image/webp" -> header.length >= 12
                        && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                        && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
                default -> false;
            };
            if (!valid) throw new BadRequestException("Payment proof content does not match its file type");
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read the payment proof");
        }
    }

    private Path safePath(String storageName) {
        if (storageName == null || storageName.isBlank() || storageName.contains("://")) {
            throw new ResourceNotFoundException("Payment proof file not found");
        }
        Path path = root.resolve(storageName).normalize();
        if (!path.startsWith(root)) throw new BadRequestException("Invalid payment proof path");
        return path;
    }

    private String extension(String storageName) {
        int dot = storageName.lastIndexOf('.');
        return dot < 0 ? "" : storageName.substring(dot).toLowerCase();
    }

    public record PaymentProofResource(Resource resource, MediaType mediaType) {}
}
