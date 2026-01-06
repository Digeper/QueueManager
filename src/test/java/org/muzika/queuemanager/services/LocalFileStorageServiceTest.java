package org.muzika.queuemanager.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService fileStorageService;
    private String basePath;

    @BeforeEach
    void setUp() {
        basePath = tempDir.toString();
        fileStorageService = new LocalFileStorageService(basePath);
    }

    @Test
    void testGetFile_WithAbsolutePath() throws IOException {
        // Create a test file
        Path testFile = tempDir.resolve("test-song.mp3");
        Files.write(testFile, "test content".getBytes());

        // Test with absolute path
        Resource resource = fileStorageService.getFile(testFile.toString());

        assertNotNull(resource);
        assertTrue(resource.exists());
        assertEquals("test content", new String(resource.getInputStream().readAllBytes()));
    }

    @Test
    void testGetFile_WithRelativePath() throws IOException {
        // Create a test file in a subdirectory
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectories(subDir);
        Path testFile = subDir.resolve("test-song.flac");
        Files.write(testFile, "test content".getBytes());

        // Test with relative path
        Resource resource = fileStorageService.getFile("subdir/test-song.flac");

        assertNotNull(resource);
        assertTrue(resource.exists());
        assertEquals("test content", new String(resource.getInputStream().readAllBytes()));
    }

    @Test
    void testGetFile_FileNotFound() {
        // Test with non-existent file
        assertThrows(IOException.class, () -> {
            fileStorageService.getFile("non-existent-file.mp3");
        });
    }

    @Test
    void testGetFile_PathIsDirectory() throws IOException {
        // Create a directory
        Path testDir = tempDir.resolve("test-dir");
        Files.createDirectories(testDir);

        // Test with directory path
        assertThrows(IllegalArgumentException.class, () -> {
            fileStorageService.getFile(testDir.toString());
        });
    }

    @Test
    void testGetFile_NullPath() {
        assertThrows(IllegalArgumentException.class, () -> {
            fileStorageService.getFile(null);
        });
    }

    @Test
    void testGetFile_EmptyPath() {
        assertThrows(IllegalArgumentException.class, () -> {
            fileStorageService.getFile("");
        });
    }

    @Test
    void testGetFile_WhitespacePath() {
        assertThrows(IllegalArgumentException.class, () -> {
            fileStorageService.getFile("   ");
        });
    }

    @Test
    void testFileExists_FileExists() throws IOException {
        // Create a test file
        Path testFile = tempDir.resolve("existing-file.mp3");
        Files.write(testFile, "test content".getBytes());

        assertTrue(fileStorageService.fileExists(testFile.toString()));
    }

    @Test
    void testFileExists_FileDoesNotExist() {
        assertFalse(fileStorageService.fileExists("non-existent-file.mp3"));
    }

    @Test
    void testFileExists_DirectoryExists() throws IOException {
        // Create a directory
        Path testDir = tempDir.resolve("test-dir");
        Files.createDirectories(testDir);

        // Directory exists but is not a file
        assertFalse(fileStorageService.fileExists(testDir.toString()));
    }

    @Test
    void testFileExists_RelativePath() throws IOException {
        // Create a test file in a subdirectory
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectories(subDir);
        Path testFile = subDir.resolve("test-song.wav");
        Files.write(testFile, "test content".getBytes());

        assertTrue(fileStorageService.fileExists("subdir/test-song.wav"));
    }

    @Test
    void testFileExists_AbsolutePath() throws IOException {
        // Create a test file
        Path testFile = tempDir.resolve("absolute-file.aiff");
        Files.write(testFile, "test content".getBytes());

        assertTrue(fileStorageService.fileExists(testFile.toString()));
    }

    @Test
    void testFileExists_NullPath() {
        assertFalse(fileStorageService.fileExists(null));
    }

    @Test
    void testFileExists_EmptyPath() {
        assertFalse(fileStorageService.fileExists(""));
    }

    @Test
    void testFileExists_WhitespacePath() {
        assertFalse(fileStorageService.fileExists("   "));
    }

    @Test
    void testPathResolution_WithParentDirectory() throws IOException {
        // Create a test file
        Path testFile = tempDir.resolve("test.mp3");
        Files.write(testFile, "test".getBytes());

        // Test with path containing parent directory reference
        Path parentPath = tempDir.resolve("..").resolve(tempDir.getFileName()).resolve("test.mp3");
        Resource resource = fileStorageService.getFile(parentPath.toString());

        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    void testPathResolution_WithCurrentDirectory() throws IOException {
        // Create a test file
        Path testFile = tempDir.resolve("test.mp3");
        Files.write(testFile, "test".getBytes());

        // Test with path containing current directory reference
        Path currentPath = tempDir.resolve(".").resolve("test.mp3");
        Resource resource = fileStorageService.getFile(currentPath.toString());

        assertNotNull(resource);
        assertTrue(resource.exists());
    }
}

