package com.jadhavr.erp.college.service;

import com.jadhavr.erp.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class CollegeImageStorageService {
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");
    private final Path directory;

    public CollegeImageStorageService(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
        directory = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("college");
    }

    public String store(MultipartFile file, String kind) {
        if (!"logo".equals(kind) && !"qr-code".equals(kind))
            throw new BadRequestException("Image type must be logo or qr-code");
        if (file == null || file.isEmpty()) throw new BadRequestException("Please select an image");
        if (file.getSize() > MAX_SIZE) throw new BadRequestException("Image must not exceed 5 MB");
        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) throw new BadRequestException("Only JPG, PNG, and WebP images are allowed");
        verifySignature(file, file.getContentType());
        String filename = kind + "-" + UUID.randomUUID() + extension;
        Path destination = directory.resolve(filename).normalize();
        if (!destination.getParent().equals(directory)) throw new BadRequestException("Invalid image filename");
        try {
            Files.createDirectories(directory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/college/" + filename;
        } catch (IOException exception) {
            throw new BadRequestException("Could not save the college image");
        }
    }

    private void verifySignature(MultipartFile file, String type) {
        try (InputStream input = file.getInputStream()) {
            byte[] h = input.readNBytes(12);
            boolean valid = switch (type) {
                case "image/jpeg" -> h.length >= 3 && u(h[0]) == 0xff && u(h[1]) == 0xd8 && u(h[2]) == 0xff;
                case "image/png" -> h.length >= 8 && u(h[0]) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
                case "image/webp" -> h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                        && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
                default -> false;
            };
            if (!valid) throw new BadRequestException("The selected file is not a valid image");
        } catch (IOException exception) {
            throw new BadRequestException("Could not read the college image");
        }
    }

    private int u(byte value) { return value & 0xff; }
}
