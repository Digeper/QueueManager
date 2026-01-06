package org.muzika.queuemanager.services;


import jakarta.transaction.Transactional;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.kafkaMassages.RequestSlskdSong;
import org.muzika.queuemanager.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
}
