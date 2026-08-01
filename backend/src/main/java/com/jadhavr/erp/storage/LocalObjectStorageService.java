package com.jadhavr.erp.storage;

import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorageService implements ObjectStorageService {
    private final Path root;

    public LocalObjectStorageService(@Value("${app.upload-dir:uploads}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize().resolve("objects");
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            Path metadata = target.resolveSibling(target.getFileName() + ".content-type");
            Files.writeString(metadata, contentType);
            restrictPermissions(target);
            restrictPermissions(metadata);
        } catch (IOException exception) {
            throw new BadRequestException("Unable to store the uploaded file");
        }
    }

    private void restrictPermissions(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException ignored) {
            // Windows ACLs are inherited from the isolated upload directory.
            path.toFile().setExecutable(false, false);
        }
    }

    @Override
    public StoredObject get(String key) {
        Path target = resolve(key);
        try {
            if (!Files.isRegularFile(target)) throw new ResourceNotFoundException("Stored file not found");
            Path metadata = target.resolveSibling(target.getFileName() + ".content-type");
            String contentType = Files.isRegularFile(metadata)
                    ? Files.readString(metadata) : "application/octet-stream";
            return new StoredObject(Files.readAllBytes(target), contentType);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Stored file not found");
        }
    }

    @Override
    public void delete(String key) {
        Path target = resolve(key);
        try {
            Files.deleteIfExists(target);
            Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".content-type"));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to delete stored file", exception);
        }
    }

    private Path resolve(String key) {
        Path path = root.resolve(ObjectKeyPolicy.requireSafe(key)).normalize();
        if (!path.startsWith(root)) throw new BadRequestException("Invalid storage object key");
        return path;
    }
}
