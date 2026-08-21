package com.collegeerp.erp.storage;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

/** Compatibility facade for image-only upload services. */
@Component
public class ImageUploadValidator {
    private final SecureFileContentValidator validator;

    public ImageUploadValidator() {
        this(new SecureFileContentValidator());
    }

    @Autowired
    public ImageUploadValidator(SecureFileContentValidator validator) {
        this.validator = validator;
    }

    public ValidatedImage validate(MultipartFile file, long maximumBytes) {
        SecureFileContentValidator.ValidatedContent image =
                validator.validateImage(file, maximumBytes);
        return new ValidatedImage(
                image.content(), image.contentType(), image.extension());
    }

    public record ValidatedImage(byte[] content, String contentType, String extension) {
    }
}
