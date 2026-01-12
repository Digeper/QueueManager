package org.muzika.queuemanager.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.services.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = QueueController.class)
class QueueControllerSongTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueueService queueService;

    @MockitoBean
    private QueueCheckerService queueCheckerService;

    @MockitoBean
    private SongService songService;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @TempDir
    Path tempDir;

    private UUID songId;
    private Song song;
    private Resource mockResource;

    @BeforeEach
    void setUp() throws IOException {
        songId = UUID.randomUUID();
        song = new Song();
        song.setId(songId);
        song.setTitle("Test Song");
        song.setArtist("Test Artist");
        song.setUrl("test-song.mp3");

        // Create a real file for testing
        Path testFile = tempDir.resolve("test-song.mp3");
        Files.write(testFile, "fake mp3 content".getBytes());
        mockResource = new FileSystemResource(testFile);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSong_Success() throws Exception {
        // Mock song retrieval
        when(songService.findByUUID(songId)).thenReturn(song);
        
        // Mock file storage
        when(fileStorageService.getFile("test-song.mp3")).thenReturn(mockResource);

        mockMvc.perform(get("/api/queue/songs/{id}", songId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.parseMediaType("audio/mpeg").toString()))
                .andExpect(header().exists(HttpHeaders.CONTENT_LENGTH))
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION));

        verify(songService, times(1)).findByUUID(songId);
        verify(fileStorageService, times(1)).getFile("test-song.mp3");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSong_SongNotFound() throws Exception {
        // Mock song not found
        when(songService.findByUUID(songId)).thenThrow(new RuntimeException("Song not found"));

        mockMvc.perform(get("/api/queue/songs/{id}", songId))
                .andExpect(status().isNotFound());

        verify(songService, times(1)).findByUUID(songId);
        verify(fileStorageService, never()).getFile(anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSong_FileNotFound() throws Exception {
        // Mock song retrieval
        when(songService.findByUUID(songId)).thenReturn(song);
        
        // Mock file not found
        when(fileStorageService.getFile("test-song.mp3")).thenThrow(new IOException("File not found"));

        mockMvc.perform(get("/api/queue/songs/{id}", songId))
                .andExpect(status().isNotFound());

        verify(songService, times(1)).findByUUID(songId);
        verify(fileStorageService, times(1)).getFile("test-song.mp3");
    }

    @Test
    void testGetSong_Unauthorized() throws Exception {
        // Clear security context to simulate unauthenticated user
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/queue/songs/{id}", songId))
                .andExpect(status().isUnauthorized());

        verify(songService, never()).findByUUID(any());
        verify(fileStorageService, never()).getFile(anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSong_NoFilePath() throws Exception {
        // Song with null URL
        song.setUrl(null);
        when(songService.findByUUID(songId)).thenReturn(song);

        mockMvc.perform(get("/api/queue/songs/{id}", songId))
                .andExpect(status().isNotFound());

        verify(songService, times(1)).findByUUID(songId);
        verify(fileStorageService, never()).getFile(anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSong_EmptyFilePath() throws Exception {
        // Song with empty URL
        song.setUrl("");
        when(songService.findByUUID(songId)).thenReturn(song);

        mockMvc.perform(get("/api/queue/songs/{id}", songId))
                .andExpect(status().isNotFound());

        verify(songService, times(1)).findByUUID(songId);
        verify(fileStorageService, never()).getFile(anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSong_FileStorageException() throws Exception {
        // Mock song retrieval
        when(songService.findByUUID(songId)).thenReturn(song);
        
        // Mock invalid path exception
        when(fileStorageService.getFile("test-song.mp3")).thenThrow(new IllegalArgumentException("Invalid path"));

        mockMvc.perform(get("/api/queue/songs/{id}", songId))
                .andExpect(status().isInternalServerError());

        verify(songService, times(1)).findByUUID(songId);
        verify(fileStorageService, times(1)).getFile("test-song.mp3");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSong_ContentType_MP3() throws Exception {
        song.setUrl("test-song.mp3");
        when(songService.findByUUID(songId)).thenReturn(song);
        when(fileStorageService.getFile("test-song.mp3")).thenReturn(mockResource);

        mockMvc.perform(get("/api/queue/songs/{id}", songId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.parseMediaType("audio/mpeg").toString()));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSong_ContentType_FLAC() throws Exception {
        song.setUrl("test-song.flac");
        Path flacFile = tempDir.resolve("test-song.flac");
        Files.write(flacFile, "fake flac content".getBytes());
        Resource flacResource = new FileSystemResource(flacFile);

        when(songService.findByUUID(songId)).thenReturn(song);
        when(fileStorageService.getFile("test-song.flac")).thenReturn(flacResource);

        mockMvc.perform(get("/api/queue/songs/{id}", songId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.parseMediaType("audio/flac").toString()));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSong_ContentType_WAV() throws Exception {
        song.setUrl("test-song.wav");
        Path wavFile = tempDir.resolve("test-song.wav");
        Files.write(wavFile, "fake wav content".getBytes());
        Resource wavResource = new FileSystemResource(wavFile);

        when(songService.findByUUID(songId)).thenReturn(song);
        when(fileStorageService.getFile("test-song.wav")).thenReturn(wavResource);

        mockMvc.perform(get("/api/queue/songs/{id}", songId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.parseMediaType("audio/wav").toString()));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSong_ContentType_AIFF() throws Exception {
        song.setUrl("test-song.aiff");
        Path aiffFile = tempDir.resolve("test-song.aiff");
        Files.write(aiffFile, "fake aiff content".getBytes());
        Resource aiffResource = new FileSystemResource(aiffFile);

        when(songService.findByUUID(songId)).thenReturn(song);
        when(fileStorageService.getFile("test-song.aiff")).thenReturn(aiffResource);

        mockMvc.perform(get("/api/queue/songs/{id}", songId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.parseMediaType("audio/aiff").toString()));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSong_ContentType_Unknown() throws Exception {
        song.setUrl("test-song.unknown");
        Path unknownFile = tempDir.resolve("test-song.unknown");
        Files.write(unknownFile, "fake content".getBytes());
        Resource unknownResource = new FileSystemResource(unknownFile);

        when(songService.findByUUID(songId)).thenReturn(song);
        when(fileStorageService.getFile("test-song.unknown")).thenReturn(unknownResource);

        mockMvc.perform(get("/api/queue/songs/{id}", songId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM.toString()));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSong_FilenameExtraction() throws Exception {
        song.setUrl("/path/to/test-song.mp3");
        when(songService.findByUUID(songId)).thenReturn(song);
        when(fileStorageService.getFile("/path/to/test-song.mp3")).thenReturn(mockResource);

        mockMvc.perform(get("/api/queue/songs/{id}", songId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test-song.mp3\""));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSong_UnexpectedException() throws Exception {
        // Mock unexpected exception
        when(songService.findByUUID(songId)).thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/api/queue/songs/{id}", songId))
                .andExpect(status().isNotFound());

        verify(songService, times(1)).findByUUID(songId);
    }
}

