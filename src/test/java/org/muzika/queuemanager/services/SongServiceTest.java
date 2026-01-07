package org.muzika.queuemanager.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.repository.SongRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock
    private SongRepository songRepository;

    @InjectMocks
    private SongService songService;

    private Song song1;
    private Song song2;
    private Song song3;
    private Song song4;
    private Song song5;
    private Song songWithoutUrl;
    private Song songWithEmptyUrl;

    @BeforeEach
    void setUp() {
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

        song4 = new Song();
        song4.setId(UUID.randomUUID());
        song4.setTitle("Song 4");
        song4.setUrl("/path/to/song4.wav");

        song5 = new Song();
        song5.setId(UUID.randomUUID());
        song5.setTitle("Song 5");
        song5.setUrl("/path/to/song5.aiff");

        songWithoutUrl = new Song();
        songWithoutUrl.setId(UUID.randomUUID());
        songWithoutUrl.setTitle("Song Without URL");
        songWithoutUrl.setUrl(null);

        songWithEmptyUrl = new Song();
        songWithEmptyUrl.setId(UUID.randomUUID());
        songWithEmptyUrl.setTitle("Song With Empty URL");
        songWithEmptyUrl.setUrl("");
    }

    @Test
    void testFindRandomSongsWithUrl_ReturnsRequestedLimit() {
        // Arrange
        List<Song> allSongsWithUrl = Arrays.asList(song1, song2, song3, song4, song5);
        when(songRepository.findAllByUrlIsNotNullAndNotEmpty()).thenReturn(allSongsWithUrl);

        // Act
        List<Song> result = songService.findRandomSongsWithUrl(3);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(songRepository, times(1)).findAllByUrlIsNotNullAndNotEmpty();
    }

    @Test
    void testFindRandomSongsWithUrl_ReturnsAllWhenLessThanLimit() {
        // Arrange
        List<Song> allSongsWithUrl = Arrays.asList(song1, song2);
        when(songRepository.findAllByUrlIsNotNullAndNotEmpty()).thenReturn(allSongsWithUrl);

        // Act
        List<Song> result = songService.findRandomSongsWithUrl(10);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(song1));
        assertTrue(result.contains(song2));
        verify(songRepository, times(1)).findAllByUrlIsNotNullAndNotEmpty();
    }

    @Test
    void testFindRandomSongsWithUrl_ReturnsEmptyListWhenNoSongsWithUrl() {
        // Arrange
        when(songRepository.findAllByUrlIsNotNullAndNotEmpty()).thenReturn(Collections.emptyList());

        // Act
        List<Song> result = songService.findRandomSongsWithUrl(10);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(songRepository, times(1)).findAllByUrlIsNotNullAndNotEmpty();
    }

    @Test
    void testFindRandomSongsWithUrl_ReturnsExactCountWhenEqual() {
        // Arrange
        List<Song> allSongsWithUrl = Arrays.asList(song1, song2, song3, song4, song5);
        when(songRepository.findAllByUrlIsNotNullAndNotEmpty()).thenReturn(allSongsWithUrl);

        // Act
        List<Song> result = songService.findRandomSongsWithUrl(5);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());
        verify(songRepository, times(1)).findAllByUrlIsNotNullAndNotEmpty();
    }

    @Test
    void testFindRandomSongsWithUrl_ReturnsRandomSelection() {
        // Arrange
        List<Song> allSongsWithUrl = Arrays.asList(song1, song2, song3, song4, song5);
        when(songRepository.findAllByUrlIsNotNullAndNotEmpty()).thenReturn(allSongsWithUrl);

        // Act - call multiple times to verify randomness
        List<Song> result1 = songService.findRandomSongsWithUrl(3);
        List<Song> result2 = songService.findRandomSongsWithUrl(3);
        List<Song> result3 = songService.findRandomSongsWithUrl(3);

        // Assert - at least one result should be different (very likely with shuffling)
        // We can't guarantee randomness, but we can verify the size and that all songs have URLs
        assertEquals(3, result1.size());
        assertEquals(3, result2.size());
        assertEquals(3, result3.size());
        
        // Verify all returned songs have URLs
        for (Song song : result1) {
            assertNotNull(song.getUrl());
            assertFalse(song.getUrl().isEmpty());
        }
        
        verify(songRepository, times(3)).findAllByUrlIsNotNullAndNotEmpty();
    }

    @Test
    void testFindRandomSongsWithUrl_WithZeroLimit() {
        // Arrange
        List<Song> allSongsWithUrl = Arrays.asList(song1, song2, song3);
        when(songRepository.findAllByUrlIsNotNullAndNotEmpty()).thenReturn(allSongsWithUrl);

        // Act
        List<Song> result = songService.findRandomSongsWithUrl(0);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(songRepository, times(1)).findAllByUrlIsNotNullAndNotEmpty();
    }

    @Test
    void testFindRandomSongsWithUrl_WithSingleSong() {
        // Arrange
        List<Song> singleSong = Collections.singletonList(song1);
        when(songRepository.findAllByUrlIsNotNullAndNotEmpty()).thenReturn(singleSong);

        // Act
        List<Song> result = songService.findRandomSongsWithUrl(10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(song1.getId(), result.get(0).getId());
        verify(songRepository, times(1)).findAllByUrlIsNotNullAndNotEmpty();
    }
}

