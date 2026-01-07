package org.muzika.queuemanager.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.persistence.EntityManager;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.entities.User;
import org.muzika.queuemanager.entities.UserSong;
import org.muzika.queuemanager.entities.UserSongId;
import org.muzika.queuemanager.repository.QueueRepository;
import org.muzika.queuemanager.repository.UserRepository;
import org.muzika.queuemanager.repository.UserSongRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceCollectionTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private UserSongRepository userSongRepository;

    @Mock
    private SongService songService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Song song1;
    private Song song2;
    private Song song3;
    private String username;
    private UUID userId;

    @BeforeEach
    void setUp() {
        username = "testuser";
        userId = UUID.randomUUID();

        testUser = new User();
        testUser.setUuid(userId);
        testUser.setUserName(username);

        song1 = new Song();
        song1.setId(UUID.randomUUID());
        song1.setTitle("Song 1");
        song1.setUrl("/path/to/song1.mp3");

        song2 = new Song();
        song2.setId(UUID.randomUUID());
        song2.setTitle("Song 2");
        song2.setUrl("/path/to/song2.flac");

        song3 = new Song();
        song3.setId(UUID.randomUUID());
        song3.setTitle("Song 3");
        song3.setUrl("/path/to/song3.mp3");
    }

    @Test
    void testAddSongsToUserCollection_AddsNewSongs() {
        // Arrange
        List<Song> songs = Arrays.asList(song1, song2, song3);
        when(userRepository.findByUserName(username)).thenReturn(testUser);
        
        // Mock that UserSongs don't exist yet
        when(userSongRepository.findById(any(UserSongId.class))).thenReturn(Optional.empty());
        when(userSongRepository.save(any(UserSong.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.addSongsToUserCollection(username, songs);

        // Assert
        verify(userRepository, times(1)).findByUserName(username);
        verify(userSongRepository, times(3)).findById(any(UserSongId.class));
        verify(userSongRepository, times(3)).save(any(UserSong.class));
    }

    @Test
    void testAddSongsToUserCollection_SkipsExistingSongs() {
        // Arrange
        List<Song> songs = Arrays.asList(song1, song2);
        when(userRepository.findByUserName(username)).thenReturn(testUser);
        
        // Mock that song1 already exists, song2 doesn't
        UserSong existingUserSong = new UserSong();
        existingUserSong.setUserId(userId);
        existingUserSong.setSongId(song1.getId());
        
        when(userSongRepository.findById(any(UserSongId.class))).thenAnswer(invocation -> {
            UserSongId id = invocation.getArgument(0);
            if (id.getSongId().equals(song1.getId())) {
                return Optional.of(existingUserSong);
            } else {
                return Optional.empty();
            }
        });
        
        when(userSongRepository.save(any(UserSong.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.addSongsToUserCollection(username, songs);

        // Assert
        verify(userRepository, times(1)).findByUserName(username);
        verify(userSongRepository, times(2)).findById(any(UserSongId.class));
        // Only song2 should be saved (song1 already exists)
        verify(userSongRepository, times(1)).save(any(UserSong.class));
    }

    @Test
    void testAddSongsToUserCollection_WithEmptyList() {
        // Arrange
        List<Song> emptyList = Collections.emptyList();
        when(userRepository.findByUserName(username)).thenReturn(testUser);

        // Act
        userService.addSongsToUserCollection(username, emptyList);

        // Assert
        verify(userRepository, times(1)).findByUserName(username);
        verify(userSongRepository, never()).findById(any(UserSongId.class));
        verify(userSongRepository, never()).save(any(UserSong.class));
    }

    @Test
    void testAddSongsToUserCollection_UserNotFound() {
        // Arrange
        List<Song> songs = Arrays.asList(song1);
        when(userRepository.findByUserName(username)).thenReturn(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.addSongsToUserCollection(username, songs)
        );

        assertEquals("User not found: " + username, exception.getMessage());
        verify(userRepository, times(1)).findByUserName(username);
        verify(userSongRepository, never()).findById(any(UserSongId.class));
        verify(userSongRepository, never()).save(any(UserSong.class));
    }

    @Test
    void testAddSongsToUserCollection_AllSongsAlreadyExist() {
        // Arrange
        List<Song> songs = Arrays.asList(song1, song2);
        when(userRepository.findByUserName(username)).thenReturn(testUser);
        
        // Mock that all songs already exist
        UserSong existingUserSong1 = new UserSong();
        existingUserSong1.setUserId(userId);
        existingUserSong1.setSongId(song1.getId());
        
        UserSong existingUserSong2 = new UserSong();
        existingUserSong2.setUserId(userId);
        existingUserSong2.setSongId(song2.getId());
        
        when(userSongRepository.findById(any(UserSongId.class))).thenAnswer(invocation -> {
            UserSongId id = invocation.getArgument(0);
            if (id.getSongId().equals(song1.getId())) {
                return Optional.of(existingUserSong1);
            } else if (id.getSongId().equals(song2.getId())) {
                return Optional.of(existingUserSong2);
            }
            return Optional.empty();
        });

        // Act
        userService.addSongsToUserCollection(username, songs);

        // Assert
        verify(userRepository, times(1)).findByUserName(username);
        verify(userSongRepository, times(2)).findById(any(UserSongId.class));
        // No songs should be saved since they all already exist
        verify(userSongRepository, never()).save(any(UserSong.class));
    }

    @Test
    void testAddSongsToUserCollection_WithSingleSong() {
        // Arrange
        List<Song> singleSong = Collections.singletonList(song1);
        when(userRepository.findByUserName(username)).thenReturn(testUser);
        when(userSongRepository.findById(any(UserSongId.class))).thenReturn(Optional.empty());
        when(userSongRepository.save(any(UserSong.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.addSongsToUserCollection(username, singleSong);

        // Assert
        verify(userRepository, times(1)).findByUserName(username);
        verify(userSongRepository, times(1)).findById(any(UserSongId.class));
        verify(userSongRepository, times(1)).save(any(UserSong.class));
    }
}

