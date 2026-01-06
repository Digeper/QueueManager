package org.muzika.queuemanager.services;

import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * Service interface for file storage operations.
 * Provides methods to retrieve and check existence of files.
 */
public interface FileStorageService {
    
    /**
     * Retrieves a file as a Resource from the storage.
     * 
     * @param filePath The path to the file. Can be absolute or relative to the base storage path.
     * @return A Resource representing the file
     * @throws IOException if the file cannot be read or does not exist
     * @throws IllegalArgumentException if the path is invalid or points to a directory
     */
    Resource getFile(String filePath) throws IOException;
    
    /**
     * Checks if a file exists in the storage.
     * 
     * @param filePath The path to the file. Can be absolute or relative to the base storage path.
     * @return true if the file exists and is a regular file, false otherwise
     */
    boolean fileExists(String filePath);
}

