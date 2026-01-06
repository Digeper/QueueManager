package org.muzika.queuemanager.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.muzika.queuemanager.entities.Queue;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.entities.User;
import org.muzika.queuemanager.repository.QueueRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private UserService userService;

    @Mock
    private SongService songService;

    @InjectMocks
    private QueueService queueService;

    private User testUser;
    private Queue testQueue;
    private UUID userId;
    private UUID song1Id;
    private UUID song2Id;
    private UUID song3Id;
    private Song song1;
    private Song song2;
    private Song song3;
    private String username;

    @BeforeEach
    void setUp() {
        username = "testuser";
        userId = UUID.randomUUID();
        song1Id = UUID.randomUUID();
        song2Id = UUID.randomUUID();
        song3Id = UUID.randomUUID();

        // Create test user
        testUser = new User();
        testUser.setUuid(userId);
        testUser.setUserName(username);

        // Create test songs
        song1 = new Song();
        song1.setId(song1Id);
        song1.setTitle("Song 1");
        song1.setArtist("Artist 1");

        song2 = new Song();
        song2.setId(song2Id);
        song2.setTitle("Song 2");
        song2.setArtist("Artist 2");

        song3 = new Song();
        song3.setId(song3Id);
        song3.setTitle("Song 3");
        song3.setArtist("Artist 3");

        // Create test queue with songs
        testQueue = new Queue();
        testQueue.setUserUuid(userId);
        testQueue.setUuid(userId);
        testQueue.setUser(testUser);
        List<Song> songs = new ArrayList<>();
        songs.add(song1);
        songs.add(song2);
        songs.add(song3);
        testQueue.setSongs(songs);
    }

    @Test
    void testRemoveSongFromQueue_SongExists_RemovesOnlySpecifiedSong() {
        // Arrange
        when(queueRepository.findByUser_UserName(username)).thenReturn(testQueue);
        when(queueRepository.save(any(Queue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        queueService.removeSongFromQueue(username, song2Id);

        // Assert
        verify(queueRepository, times(1)).save(any(Queue.class));
        Queue savedQueue = testQueue;
        List<Song> remainingSongs = savedQueue.getSongs();
        
        assertNotNull(remainingSongs);
        assertEquals(2, remainingSongs.size(), "Should have 2 songs remaining");
        assertTrue(remainingSongs.contains(song1), "Should contain song1");
        assertTrue(remainingSongs.contains(song3), "Should contain song3");
        assertFalse(remainingSongs.contains(song2), "Should not contain song2");
        
        // Verify order is maintained
        assertEquals(song1Id, remainingSongs.get(0).getId(), "First song should be song1");
        assertEquals(song3Id, remainingSongs.get(1).getId(), "Second song should be song3");
    }

    @Test
    void testRemoveSongFromQueue_SongDoesNotExist_NoChanges() {
        // Arrange
        UUID nonExistentSongId = UUID.randomUUID();
        when(queueRepository.findByUser_UserName(username)).thenReturn(testQueue);

        int originalSize = testQueue.getSongs().size();

        // Act
        queueService.removeSongFromQueue(username, nonExistentSongId);

        // Assert
        verify(queueRepository, never()).save(any(Queue.class));
        assertEquals(originalSize, testQueue.getSongs().size(), "Queue size should remain unchanged");
        assertTrue(testQueue.getSongs().contains(song1));
        assertTrue(testQueue.getSongs().contains(song2));
        assertTrue(testQueue.getSongs().contains(song3));
    }

    @Test
    void testRemoveSongFromQueue_EmptyQueue_NoChanges() {
        // Arrange
        Queue emptyQueue = new Queue();
        emptyQueue.setUserUuid(userId);
        emptyQueue.setUuid(userId);
        emptyQueue.setUser(testUser);
        emptyQueue.setSongs(new ArrayList<>());

        when(queueRepository.findByUser_UserName(username)).thenReturn(emptyQueue);

        // Act
        queueService.removeSongFromQueue(username, song1Id);

        // Assert
        verify(queueRepository, never()).save(any(Queue.class));
        assertTrue(emptyQueue.getSongs().isEmpty(), "Queue should remain empty");
    }

    @Test
    void testRemoveSongFromQueue_NullSongsList_NoChanges() {
        // Arrange
        Queue queueWithNullSongs = new Queue();
        queueWithNullSongs.setUserUuid(userId);
        queueWithNullSongs.setUuid(userId);
        queueWithNullSongs.setUser(testUser);
        queueWithNullSongs.setSongs(null);

        when(queueRepository.findByUser_UserName(username)).thenReturn(queueWithNullSongs);

        // Act
        queueService.removeSongFromQueue(username, song1Id);

        // Assert
        verify(queueRepository, never()).save(any(Queue.class));
    }

    @Test
    void testRemoveSongFromQueue_RemoveFirstSong_MaintainsOrder() {
        // Arrange
        when(queueRepository.findByUser_UserName(username)).thenReturn(testQueue);
        when(queueRepository.save(any(Queue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        queueService.removeSongFromQueue(username, song1Id);

        // Assert
        verify(queueRepository, times(1)).save(any(Queue.class));
        List<Song> remainingSongs = testQueue.getSongs();
        assertEquals(2, remainingSongs.size());
        assertEquals(song2Id, remainingSongs.get(0).getId(), "First song should be song2");
        assertEquals(song3Id, remainingSongs.get(1).getId(), "Second song should be song3");
    }

    @Test
    void testRemoveSongFromQueue_RemoveLastSong_MaintainsOrder() {
        // Arrange
        when(queueRepository.findByUser_UserName(username)).thenReturn(testQueue);
        when(queueRepository.save(any(Queue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        queueService.removeSongFromQueue(username, song3Id);

        // Assert
        verify(queueRepository, times(1)).save(any(Queue.class));
        List<Song> remainingSongs = testQueue.getSongs();
        assertEquals(2, remainingSongs.size());
        assertEquals(song1Id, remainingSongs.get(0).getId(), "First song should be song1");
        assertEquals(song2Id, remainingSongs.get(1).getId(), "Second song should be song2");
    }

    @Test
    void testRemoveSongFromQueue_RemoveMiddleSong_MaintainsOrder() {
        // Arrange
        when(queueRepository.findByUser_UserName(username)).thenReturn(testQueue);
        when(queueRepository.save(any(Queue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        queueService.removeSongFromQueue(username, song2Id);

        // Assert
        verify(queueRepository, times(1)).save(any(Queue.class));
        List<Song> remainingSongs = testQueue.getSongs();
        assertEquals(2, remainingSongs.size());
        assertEquals(song1Id, remainingSongs.get(0).getId(), "First song should be song1");
        assertEquals(song3Id, remainingSongs.get(1).getId(), "Second song should be song3");
    }

    @Test
    void testRemoveSongFromQueue_SingleSongQueue_BecomesEmpty() {
        // Arrange
        Queue singleSongQueue = new Queue();
        singleSongQueue.setUserUuid(userId);
        singleSongQueue.setUuid(userId);
        singleSongQueue.setUser(testUser);
        List<Song> singleSong = new ArrayList<>();
        singleSong.add(song1);
        singleSongQueue.setSongs(singleSong);

        when(queueRepository.findByUser_UserName(username)).thenReturn(singleSongQueue);
        when(queueRepository.save(any(Queue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        queueService.removeSongFromQueue(username, song1Id);

        // Assert
        verify(queueRepository, times(1)).save(any(Queue.class));
        assertTrue(singleSongQueue.getSongs().isEmpty(), "Queue should be empty after removing the only song");
    }
}

