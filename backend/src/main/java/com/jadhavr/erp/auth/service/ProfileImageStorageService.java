package com.jadhavr.erp.auth.service;

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
public class ProfileImageStorageService {
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path profileDirectory;

    public ProfileImageStorageService(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
        this.profileDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("profile");
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a profile photo");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("Profile photo must not exceed 5 MB");
        }

        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new BadRequestException("Only JPG, PNG, and WebP images are allowed");
        }

        verifyImageSignature(file, file.getContentType());
        String filename = UUID.randomUUID() + extension;
        Path destination = profileDirectory.resolve(filename).normalize();
        if (!destination.getParent().equals(profileDirectory)) {
            throw new BadRequestException("Invalid profile photo filename");
        }

        try {
            Files.createDirectories(profileDirectory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/profile/" + filename;
        } catch (IOException exception) {
            throw new BadRequestException("Could not save the profile photo");
        }
    }

    public void deleteManagedFile(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/profile/")) return;
        String filename = imageUrl.substring("/uploads/profile/".length());
        Path file = profileDirectory.resolve(filename).normalize();
        if (!file.getParent().equals(profileDirectory)) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // A stale image must not prevent the newly uploaded photo from being used.
        }
    }

    private void verifyImageSignature(MultipartFile file, String contentType) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            boolean valid = switch (contentType) {
                case "image/jpeg" -> header.length >= 3 && unsigned(header[0]) == 0xFF
                        && unsigned(header[1]) == 0xD8 && unsigned(header[2]) == 0xFF;
                case "image/png" -> header.length >= 8 && unsigned(header[0]) == 0x89
                        && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                        && unsigned(header[4]) == 0x0D && unsigned(header[5]) == 0x0A
                        && unsigned(header[6]) == 0x1A && unsigned(header[7]) == 0x0A;
                case "image/webp" -> header.length >= 12 && header[0] == 'R' && header[1] == 'I'
                        && header[2] == 'F' && header[3] == 'F' && header[8] == 'W'
                        && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
                default -> false;
            };
            if (!valid) throw new BadRequestException("The selected file is not a valid image");
        } catch (IOException exception) {
            throw new BadRequestException("Could not read the profile photo");
        }
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }
}
