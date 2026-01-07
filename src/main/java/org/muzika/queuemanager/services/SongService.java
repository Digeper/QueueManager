package org.muzika.queuemanager.services;


import jakarta.transaction.Transactional;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.kafkaMassages.RequestSlskdSong;
import org.muzika.queuemanager.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class SongService {
    private final SongRepository songRepository;

    @Autowired
    public SongService(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    public void save(Song song) {
        songRepository.save(song);
    }

    public void updateSongPath(UUID uuid, String filePath) {
        Song song = songRepository.getReferenceById(uuid);
        song.setUrl(filePath);
        songRepository.save(song);
    }

    public void delete(UUID uuid) {
        songRepository.deleteById(uuid);
    }

    public Song findByUUID(UUID songId) {
        // Option A: Use JOIN FETCH to avoid lazy loading issues
        return songRepository.findByIdWithUserSongs(songId) .orElseThrow(RuntimeException::new);
    }

    public void updateSongName(RequestSlskdSong requestSlskdSong) {
        Song song = songRepository.getReferenceById(requestSlskdSong.getId());
        song.setArtist(requestSlskdSong.getArtist());
        song.setTitle(requestSlskdSong.getTitle());
        songRepository.save(song);
    }

    public List<Song> findAllInvalidSongs() {
        return songRepository.findAllByUrlIsNullOrUrlIsEmpty();
    }

    /**
     * Returns a list of random songs that have a URL.
     * 
     * @param limit Optional limit for the number of songs to return. 
     *              If null or 0, returns all songs with URLs.
     * @return List of random songs that have a URL
     */
    public List<Song> getRandomSongsWithUrl(Integer limit) {
        List<Song> songsWithUrl = songRepository.findAllByUrlIsNotNull();
        Collections.shuffle(songsWithUrl);
        
        if (limit != null && limit > 0 && limit < songsWithUrl.size()) {
            return songsWithUrl.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }
        
        return songsWithUrl;
    }

    /**
     * Returns all random songs that have a URL (no limit).
     * 
     * @return List of random songs that have a URL
     */
    public List<Song> getRandomSongsWithUrl() {
        return getRandomSongsWithUrl(null);
    }

    public Song findSongById(UUID uuid) {
        return songRepository.findById(uuid) .orElseThrow(RuntimeException::new);
    }
}
