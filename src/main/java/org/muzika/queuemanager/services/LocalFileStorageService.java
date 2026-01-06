package org.muzika.queuemanager.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implementation of FileStorageService that uses the local file system.
 * Supports both absolute and relative file paths.
 */
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private final String basePath;

    public LocalFileStorageService(@Value("${music.storage.base-path:/rw/downloads}") String basePath) {
        this.basePath = basePath;
        log.info("Initialized LocalFileStorageService with base path: {}", basePath);
    }

    @Override
    public Resource getFile(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        Path resolvedPath = resolvePath(filePath);
        File file = resolvedPath.toFile();

        if (!file.exists()) {
            log.warn("File not found: {}", resolvedPath);
            throw new IOException("File not found: " + resolvedPath);
        }

        if (!file.isFile()) {
            log.warn("Path is not a file (may be a directory): {}", resolvedPath);
            throw new IllegalArgumentException("Path is not a file: " + resolvedPath);
        }

        if (!Files.isReadable(resolvedPath)) {
            log.warn("File is not readable: {}", resolvedPath);
            throw new IOException("File is not readable: " + resolvedPath);
        }

        log.debug("Retrieved file: {}", resolvedPath);
        return new FileSystemResource(resolvedPath);
    }

    @Override
    public boolean fileExists(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        try {
            Path resolvedPath = resolvePath(filePath);
            File file = resolvedPath.toFile();
            return file.exists() && file.isFile() && Files.isReadable(resolvedPath);
        } catch (Exception e) {
            log.debug("Error checking file existence for path: {}, error: {}", filePath, e.getMessage());
            return false;
        }
    }

    /**
     * Resolves a file path, handling both absolute and relative paths.
     * 
     * @param filePath The file path to resolve
     * @return The resolved Path
     */
    private Path resolvePath(String filePath) {
        Path path = Paths.get(filePath);
        
        // If the path is absolute, use it as-is
        if (path.isAbsolute()) {
            return path.normalize();
        }
        
        // Otherwise, resolve it relative to the base path
        Path base = Paths.get(basePath);
        return base.resolve(path).normalize();
    }
}

