package org.muzika.queuemanager.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.muzika.queuemanager.services.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = QueueController.class)
@AutoConfigureMockMvc(addFilters = false)
class QueueControllerLikedUnlikedTest {

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

    private UUID songId;
    private String username;

    @BeforeEach
    void setUp() {
        songId = UUID.randomUUID();
        username = "testuser";
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsLiked_Success() throws Exception {
        doNothing().when(userService).markSongAsLiked(eq(username), eq(songId));

        mockMvc.perform(post("/songs/{id}/liked", songId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(userService, times(1)).markSongAsLiked(username, songId);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsLiked_InvalidSongId() throws Exception {
        doThrow(new IllegalArgumentException("Song not found: " + songId))
                .when(userService).markSongAsLiked(eq(username), eq(songId));

        mockMvc.perform(post("/songs/{id}/liked", songId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).markSongAsLiked(username, songId);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsLiked_UserNotFound() throws Exception {
        doThrow(new IllegalArgumentException("User not found: " + username))
                .when(userService).markSongAsLiked(eq(username), eq(songId));

        mockMvc.perform(post("/songs/{id}/liked", songId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).markSongAsLiked(username, songId);
    }

    @Test
    void testMarkSongAsLiked_Unauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/songs/{id}/liked", songId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).markSongAsLiked(any(), any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsLiked_InternalServerError() throws Exception {
        doThrow(new RuntimeException("Database error"))
                .when(userService).markSongAsLiked(eq(username), eq(songId));

        mockMvc.perform(post("/songs/{id}/liked", songId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(userService, times(1)).markSongAsLiked(username, songId);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsUnliked_Success() throws Exception {
        doNothing().when(userService).markSongAsUnliked(eq(username), eq(songId));

        mockMvc.perform(post("/songs/{id}/unliked", songId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(userService, times(1)).markSongAsUnliked(username, songId);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsUnliked_InvalidSongId() throws Exception {
        doThrow(new IllegalArgumentException("Song not found: " + songId))
                .when(userService).markSongAsUnliked(eq(username), eq(songId));

        mockMvc.perform(post("/songs/{id}/unliked", songId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).markSongAsUnliked(username, songId);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsUnliked_UserNotFound() throws Exception {
        doThrow(new IllegalArgumentException("User not found: " + username))
                .when(userService).markSongAsUnliked(eq(username), eq(songId));

        mockMvc.perform(post("/songs/{id}/unliked", songId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).markSongAsUnliked(username, songId);
    }

    @Test
    void testMarkSongAsUnliked_Unauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/songs/{id}/unliked", songId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).markSongAsUnliked(any(), any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsUnliked_InternalServerError() throws Exception {
        doThrow(new RuntimeException("Database error"))
                .when(userService).markSongAsUnliked(eq(username), eq(songId));

        mockMvc.perform(post("/songs/{id}/unliked", songId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(userService, times(1)).markSongAsUnliked(username, songId);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSongLikedStatus_SongIsLiked() throws Exception {
        when(userService.isSongLiked(eq(username), eq(songId))).thenReturn(true);

        mockMvc.perform(get("/songs/{id}/liked", songId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true));

        verify(userService, times(1)).isSongLiked(username, songId);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSongLikedStatus_SongIsNotLiked() throws Exception {
        when(userService.isSongLiked(eq(username), eq(songId))).thenReturn(false);

        mockMvc.perform(get("/songs/{id}/liked", songId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false));

        verify(userService, times(1)).isSongLiked(username, songId);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSongLikedStatus_UserNotFound() throws Exception {
        doThrow(new IllegalArgumentException("User not found: " + username))
                .when(userService).isSongLiked(eq(username), eq(songId));

        mockMvc.perform(get("/songs/{id}/liked", songId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).isSongLiked(username, songId);
    }

    @Test
    void testGetSongLikedStatus_Unauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/songs/{id}/liked", songId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).isSongLiked(any(), any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetSongLikedStatus_InternalServerError() throws Exception {
        doThrow(new RuntimeException("Database error"))
                .when(userService).isSongLiked(eq(username), eq(songId));

        mockMvc.perform(get("/songs/{id}/liked", songId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(userService, times(1)).isSongLiked(username, songId);
    }
}

