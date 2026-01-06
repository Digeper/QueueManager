package org.muzika.queuemanager.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.muzika.queuemanager.dto.SongIdRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = QueueController.class)
@AutoConfigureMockMvc(addFilters = false)
class QueueControllerSkippedFinishedTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    private UUID songId;
    private String username;

    @BeforeEach
    void setUp() {
        songId = UUID.randomUUID();
        username = "testuser";
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsSkipped_Success() throws Exception {
        SongIdRequest request = new SongIdRequest();
        request.setSongId(songId);

        doNothing().when(userService).markSongAsSkipped(eq(username), eq(songId));
        doNothing().when(queueService).removeSongFromQueue(eq(username), eq(songId));
        when(queueCheckerService.ensureMinimumQueueSize(eq(username))).thenReturn(true);

        mockMvc.perform(post("/queue/skipped")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService, times(1)).markSongAsSkipped(username, songId);
        verify(queueService, times(1)).removeSongFromQueue(username, songId);
        verify(queueCheckerService, times(1)).ensureMinimumQueueSize(username);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsSkipped_MissingSongId() throws Exception {
        SongIdRequest request = new SongIdRequest();
        // songId is null

        mockMvc.perform(post("/queue/skipped")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("songId is required"));

        verify(userService, never()).markSongAsSkipped(any(), any());
        verify(queueService, never()).removeSongFromQueue(any(), any());
        verify(queueCheckerService, never()).ensureMinimumQueueSize(any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsSkipped_InvalidSongId() throws Exception {
        SongIdRequest request = new SongIdRequest();
        request.setSongId(songId);

        doThrow(new IllegalArgumentException("Song not found: " + songId))
                .when(userService).markSongAsSkipped(eq(username), eq(songId));

        mockMvc.perform(post("/queue/skipped")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).markSongAsSkipped(username, songId);
        verify(queueService, never()).removeSongFromQueue(any(), any());
        verify(queueCheckerService, never()).ensureMinimumQueueSize(any());
    }

    @Test
    void testMarkSongAsSkipped_Unauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        SongIdRequest request = new SongIdRequest();
        request.setSongId(songId);

        mockMvc.perform(post("/queue/skipped")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).markSongAsSkipped(any(), any());
        verify(queueService, never()).removeSongFromQueue(any(), any());
        verify(queueCheckerService, never()).ensureMinimumQueueSize(any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsSkipped_InternalServerError() throws Exception {
        SongIdRequest request = new SongIdRequest();
        request.setSongId(songId);

        doThrow(new RuntimeException("Database error"))
                .when(userService).markSongAsSkipped(eq(username), eq(songId));

        mockMvc.perform(post("/queue/skipped")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        verify(userService, times(1)).markSongAsSkipped(username, songId);
        verify(queueService, never()).removeSongFromQueue(any(), any());
        verify(queueCheckerService, never()).ensureMinimumQueueSize(any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsFinished_Success() throws Exception {
        SongIdRequest request = new SongIdRequest();
        request.setSongId(songId);

        doNothing().when(userService).incrementSongListenCount(eq(username), eq(songId));
        doNothing().when(queueService).removeSongFromQueue(eq(username), eq(songId));
        when(queueCheckerService.ensureMinimumQueueSize(eq(username))).thenReturn(true);

        mockMvc.perform(post("/queue/finished")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService, times(1)).incrementSongListenCount(username, songId);
        verify(queueService, times(1)).removeSongFromQueue(username, songId);
        verify(queueCheckerService, times(1)).ensureMinimumQueueSize(username);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsFinished_MissingSongId() throws Exception {
        SongIdRequest request = new SongIdRequest();
        // songId is null

        mockMvc.perform(post("/queue/finished")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("songId is required"));

        verify(userService, never()).incrementSongListenCount(any(), any());
        verify(queueService, never()).removeSongFromQueue(any(), any());
        verify(queueCheckerService, never()).ensureMinimumQueueSize(any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsFinished_InvalidSongId() throws Exception {
        SongIdRequest request = new SongIdRequest();
        request.setSongId(songId);

        doThrow(new IllegalArgumentException("Song not found: " + songId))
                .when(userService).incrementSongListenCount(eq(username), eq(songId));

        mockMvc.perform(post("/queue/finished")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).incrementSongListenCount(username, songId);
        verify(queueService, never()).removeSongFromQueue(any(), any());
        verify(queueCheckerService, never()).ensureMinimumQueueSize(any());
    }

    @Test
    void testMarkSongAsFinished_Unauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        SongIdRequest request = new SongIdRequest();
        request.setSongId(songId);

        mockMvc.perform(post("/queue/finished")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).incrementSongListenCount(any(), any());
        verify(queueService, never()).removeSongFromQueue(any(), any());
        verify(queueCheckerService, never()).ensureMinimumQueueSize(any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsFinished_InternalServerError() throws Exception {
        SongIdRequest request = new SongIdRequest();
        request.setSongId(songId);

        doThrow(new RuntimeException("Database error"))
                .when(userService).incrementSongListenCount(eq(username), eq(songId));

        mockMvc.perform(post("/queue/finished")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        verify(userService, times(1)).incrementSongListenCount(username, songId);
        verify(queueService, never()).removeSongFromQueue(any(), any());
        verify(queueCheckerService, never()).ensureMinimumQueueSize(any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsSkipped_RemovesFromQueue() throws Exception {
        SongIdRequest request = new SongIdRequest();
        request.setSongId(songId);

        doNothing().when(userService).markSongAsSkipped(eq(username), eq(songId));
        doNothing().when(queueService).removeSongFromQueue(eq(username), eq(songId));
        when(queueCheckerService.ensureMinimumQueueSize(eq(username))).thenReturn(true);

        mockMvc.perform(post("/queue/skipped")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify the order of operations
        verify(userService, times(1)).markSongAsSkipped(username, songId);
        verify(queueService, times(1)).removeSongFromQueue(username, songId);
        verify(queueCheckerService, times(1)).ensureMinimumQueueSize(username);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testMarkSongAsFinished_IncrementsListenCount() throws Exception {
        SongIdRequest request = new SongIdRequest();
        request.setSongId(songId);

        doNothing().when(userService).incrementSongListenCount(eq(username), eq(songId));
        doNothing().when(queueService).removeSongFromQueue(eq(username), eq(songId));
        when(queueCheckerService.ensureMinimumQueueSize(eq(username))).thenReturn(true);

        mockMvc.perform(post("/queue/finished")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify the order of operations
        verify(userService, times(1)).incrementSongListenCount(username, songId);
        verify(queueService, times(1)).removeSongFromQueue(username, songId);
        verify(queueCheckerService, times(1)).ensureMinimumQueueSize(username);
    }
}

