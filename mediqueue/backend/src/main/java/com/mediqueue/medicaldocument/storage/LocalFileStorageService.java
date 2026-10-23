package com.mediqueue.medicaldocument.storage;

import com.mediqueue.exception.BusinessException;
import com.mediqueue.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private final Path rootLocation;

    public LocalFileStorageService(@Value("${mediqueue.document.storage.path:storage/documents}") String storagePath) {
        this.rootLocation = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            log.info("Initialized secure local file storage at: {}", rootLocation);
        } catch (IOException e) {
            log.error("Could not initialize local document storage directory: {}", e.getMessage());
            throw new IllegalStateException("Could not initialize local document storage", e);
        }
    }

    @Override
    public void store(InputStream inputStream, String storageKey, String contentType, long size) {
        try {
            Path destinationFile = resolveSafePath(storageKey);
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored document in private storage: key={}, size={} bytes", storageKey, size);
        } catch (IOException e) {
            log.error("Failed to store file with key {}: {}", storageKey, e.getMessage());
            throw new BusinessException("Failed to store medical document file: " + e.getMessage());
        }
    }

    @Override
    public Resource loadAsResource(String storageKey) {
        try {
            Path file = resolveSafePath(storageKey);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File object not found or unreadable for key: " + storageKey);
            }
        } catch (MalformedURLException e) {
            throw new BusinessException("Malformed file URL: " + e.getMessage());
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path file = resolveSafePath(storageKey);
            Files.deleteIfExists(file);
            log.info("Deleted document from storage: key={}", storageKey);
        } catch (IOException e) {
            log.warn("Failed to delete stored file {}: {}", storageKey, e.getMessage());
        }
    }

    private Path resolveSafePath(String storageKey) {
        Path resolved = rootLocation.resolve(storageKey).normalize();
        if (!resolved.startsWith(rootLocation)) {
            // Path traversal attempt detected
            throw new SecurityException("Illegal storage key path traversal attempt: " + storageKey);
        }
        return resolved;
    }
}
