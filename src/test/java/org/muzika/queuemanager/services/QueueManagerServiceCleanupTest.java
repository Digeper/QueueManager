package org.muzika.queuemanager.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.entities.User;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueManagerServiceCleanupTest {

    @Mock
    private UserService userService;

    @Mock
    private SongService songService;

    @Mock
    private QueueService queueService;

    @InjectMocks
    private QueueManagerService queueManagerService;

    private UUID song1Id;
    private UUID song2Id;
    private UUID song3Id;
    private Song invalidSong1; // null url
    private Song invalidSong2; // empty url
    private Song validSong; // valid url

    @BeforeEach
    void setUp() {
        song1Id = UUID.randomUUID();
        song2Id = UUID.randomUUID();
        song3Id = UUID.randomUUID();

        // Create invalid song with null url
        invalidSong1 = new Song();
        invalidSong1.setId(song1Id);
        invalidSong1.setTitle("Invalid Song 1");
        invalidSong1.setArtist("Artist 1");
        invalidSong1.setUrl(null);

        // Create invalid song with empty url
        invalidSong2 = new Song();
        invalidSong2.setId(song2Id);
        invalidSong2.setTitle("Invalid Song 2");
        invalidSong2.setArtist("Artist 2");
        invalidSong2.setUrl("");

        // Create valid song with url
        validSong = new Song();
        validSong.setId(song3Id);
        validSong.setTitle("Valid Song");
        validSong.setArtist("Artist 3");
        validSong.setUrl("/path/to/valid/song.mp3");
    }

    @Test
    void testCleanupInvalidSongs_WithNullUrl_RemovesSongSafely() throws Exception {
        // Arrange
        List<Song> invalidSongs = Arrays.asList(invalidSong1);
        when(songService.findAllInvalidSongs()).thenReturn(invalidSongs);
        when(userService.count()).thenReturn(1L); // User exists, so admin won't be created

        // Act - Call init which triggers cleanup
        queueManagerService.init();

        // Assert - Verify safe deletion order
        verify(userService, times(1)).deleteAllUserSongsBySongId(song1Id);
        verify(queueService, times(1)).removeSongFromAllQueues(song1Id);
        verify(songService, times(1)).delete(song1Id);
        
        // Verify order: UserSongs deleted first, then Queues, then Song
        var inOrder = inOrder(userService, queueService, songService);
        inOrder.verify(userService).deleteAllUserSongsBySongId(song1Id);
        inOrder.verify(queueService).removeSongFromAllQueues(song1Id);
        inOrder.verify(songService).delete(song1Id);
    }

    @Test
    void testCleanupInvalidSongs_WithEmptyUrl_RemovesSongSafely() throws Exception {
        // Arrange
        List<Song> invalidSongs = Arrays.asList(invalidSong2);
        when(songService.findAllInvalidSongs()).thenReturn(invalidSongs);
        when(userService.count()).thenReturn(1L);

        // Act
        queueManagerService.init();

        // Assert
        verify(userService, times(1)).deleteAllUserSongsBySongId(song2Id);
        verify(queueService, times(1)).removeSongFromAllQueues(song2Id);
        verify(songService, times(1)).delete(song2Id);
    }

    @Test
    void testCleanupInvalidSongs_MultipleInvalidSongs_CleansUpAll() throws Exception {
        // Arrange
        List<Song> invalidSongs = Arrays.asList(invalidSong1, invalidSong2);
        when(songService.findAllInvalidSongs()).thenReturn(invalidSongs);
        when(userService.count()).thenReturn(1L);

        // Act
        queueManagerService.init();

        // Assert - Both songs should be cleaned up
        verify(userService, times(1)).deleteAllUserSongsBySongId(song1Id);
        verify(queueService, times(1)).removeSongFromAllQueues(song1Id);
        verify(songService, times(1)).delete(song1Id);

        verify(userService, times(1)).deleteAllUserSongsBySongId(song2Id);
        verify(queueService, times(1)).removeSongFromAllQueues(song2Id);
        verify(songService, times(1)).delete(song2Id);
    }

    @Test
    void testCleanupInvalidSongs_NoInvalidSongs_NoCleanup() throws Exception {
        // Arrange
        List<Song> invalidSongs = new ArrayList<>();
        when(songService.findAllInvalidSongs()).thenReturn(invalidSongs);
        when(userService.count()).thenReturn(1L);

        // Act
        queueManagerService.init();

        // Assert - No cleanup should occur
        verify(userService, never()).deleteAllUserSongsBySongId(any(UUID.class));
        verify(queueService, never()).removeSongFromAllQueues(any(UUID.class));
        verify(songService, never()).delete(any(UUID.class));
    }

    @Test
    void testCleanupInvalidSongs_ValidSongsNotIncluded_OnlyInvalidCleaned() throws Exception {
        // Arrange - Only invalid songs should be returned by findAllInvalidSongs
        List<Song> invalidSongs = Arrays.asList(invalidSong1);
        when(songService.findAllInvalidSongs()).thenReturn(invalidSongs);
        when(userService.count()).thenReturn(1L);

        // Act
        queueManagerService.init();

        // Assert - Only invalid song should be cleaned
        verify(userService, times(1)).deleteAllUserSongsBySongId(song1Id);
        verify(userService, never()).deleteAllUserSongsBySongId(song3Id);
        verify(songService, times(1)).delete(song1Id);
        verify(songService, never()).delete(song3Id);
    }

    @Test
    void testCleanupInvalidSongs_ErrorDuringCleanup_ContinuesWithOtherSongs() throws Exception {
        // Arrange
        List<Song> invalidSongs = Arrays.asList(invalidSong1, invalidSong2);
        when(songService.findAllInvalidSongs()).thenReturn(invalidSongs);
        when(userService.count()).thenReturn(1L);
        
        // Simulate error during cleanup of first song
        doThrow(new RuntimeException("Database error")).when(userService).deleteAllUserSongsBySongId(song1Id);

        // Act
        queueManagerService.init();

        // Assert - First song cleanup should fail, but second should succeed
        verify(userService, times(1)).deleteAllUserSongsBySongId(song1Id);
        verify(queueService, never()).removeSongFromAllQueues(song1Id);
        verify(songService, never()).delete(song1Id);

        // Second song should still be cleaned up
        verify(userService, times(1)).deleteAllUserSongsBySongId(song2Id);
        verify(queueService, times(1)).removeSongFromAllQueues(song2Id);
        verify(songService, times(1)).delete(song2Id);
    }

    @Test
    void testCleanupInvalidSongs_ErrorFindingInvalidSongs_DoesNotFailStartup() throws Exception {
        // Arrange
        when(songService.findAllInvalidSongs()).thenThrow(new RuntimeException("Database connection error"));
        when(userService.count()).thenReturn(1L);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> queueManagerService.init());
        
        // Verify no cleanup attempts were made
        verify(userService, never()).deleteAllUserSongsBySongId(any(UUID.class));
        verify(queueService, never()).removeSongFromAllQueues(any(UUID.class));
        verify(songService, never()).delete(any(UUID.class));
    }

    @Test
    void testCleanupInvalidSongs_ErrorRemovingFromQueue_ContinuesToDeleteSong() throws Exception {
        // Arrange
        List<Song> invalidSongs = Arrays.asList(invalidSong1);
        when(songService.findAllInvalidSongs()).thenReturn(invalidSongs);
        when(userService.count()).thenReturn(1L);
        
        // Simulate error when removing from queues
        doThrow(new RuntimeException("Queue error")).when(queueService).removeSongFromAllQueues(song1Id);

        // Act
        queueManagerService.init();

        // Assert - Should still attempt to delete the song even if queue removal fails
        verify(userService, times(1)).deleteAllUserSongsBySongId(song1Id);
        verify(queueService, times(1)).removeSongFromAllQueues(song1Id);
        verify(songService, never()).delete(song1Id); // Song deletion skipped due to error
    }

    @Test
    void testCleanupInvalidSongs_DeletionOrder_UserSongsThenQueuesThenSong() throws Exception {
        // Arrange
        List<Song> invalidSongs = Arrays.asList(invalidSong1);
        when(songService.findAllInvalidSongs()).thenReturn(invalidSongs);
        when(userService.count()).thenReturn(1L);

        // Act
        queueManagerService.init();

        // Assert - Verify the exact order of operations
        var inOrder = inOrder(userService, queueService, songService);
        inOrder.verify(userService).deleteAllUserSongsBySongId(song1Id);
        inOrder.verify(queueService).removeSongFromAllQueues(song1Id);
        inOrder.verify(songService).delete(song1Id);
    }

    @Test
    void testCleanupInvalidSongs_WithAdminUserCreation_StillPerformsCleanup() throws Exception {
        // Arrange - No users exist, so admin will be created
        List<Song> invalidSongs = Arrays.asList(invalidSong1);
        when(songService.findAllInvalidSongs()).thenReturn(invalidSongs);
        when(userService.count()).thenReturn(0L);
        
        doNothing().when(userService).save(any(User.class));

        // Act
        queueManagerService.init();

        // Assert - Cleanup should still occur after admin creation
        verify(userService, times(1)).save(any(User.class));
        verify(userService, times(1)).deleteAllUserSongsBySongId(song1Id);
        verify(queueService, times(1)).removeSongFromAllQueues(song1Id);
        verify(songService, times(1)).delete(song1Id);
    }

    @Test
    void testCleanupInvalidSongs_DirectMethodCall_WorksCorrectly() throws Exception {
        // Arrange
        List<Song> invalidSongs = Arrays.asList(invalidSong1);
        when(songService.findAllInvalidSongs()).thenReturn(invalidSongs);

        // Act - Call cleanup method directly using reflection
        Method cleanupMethod = QueueManagerService.class.getDeclaredMethod("cleanupInvalidSongs");
        cleanupMethod.setAccessible(true);
        cleanupMethod.invoke(queueManagerService);

        // Assert
        verify(userService, times(1)).deleteAllUserSongsBySongId(song1Id);
        verify(queueService, times(1)).removeSongFromAllQueues(song1Id);
        verify(songService, times(1)).delete(song1Id);
    }
}

