package org.muzika.queuemanager.services;


import jakarta.annotation.PostConstruct;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.entities.User;
import org.muzika.queuemanager.entities.UserSong;
import org.muzika.queuemanager.kafkaMassages.LoadedSong;
import org.muzika.queuemanager.kafkaMassages.RequestSlskdSong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class QueueManagerService {

    private static final Logger log = LoggerFactory.getLogger(QueueManagerService.class);

    UserService userService;
    SongService songService;
    QueueService queueService;


    public QueueManagerService(UserService  userService, SongService songService, QueueService queueService) {
        this.userService = userService;
        this.songService = songService;
        this.queueService = queueService;
    }

    @PostConstruct
    void init() {
        if (userService.count() == 0) {
            User user = new User();
            user.setUserName("admin");
            userService.save(user);

        }

        cleanupInvalidSongs();
    }

    private void cleanupInvalidSongs() {
        try {
            List<Song> invalidSongs = songService.findAllInvalidSongs();
            int cleanedCount = 0;

            for (Song song : invalidSongs) {
                try {
                    UUID songId = song.getId();
                    
                    // Step 1: Remove from UserSongs (safe deletion order)
                    userService.deleteAllUserSongsBySongId(songId);
                    
                    // Step 2: Remove from all Queues
                    queueService.removeSongFromAllQueues(songId);
                    
                    // Step 3: Delete the Song entity
                    songService.delete(songId);
                    
                    cleanedCount++;
                } catch (Exception e) {
                    log.warn("Error cleaning up song with ID {}: {}", song.getId(), e.getMessage());
                }
            }

            if (cleanedCount > 0) {
                log.info("Cleaned up {} invalid song(s) at startup", cleanedCount);
            }
        } catch (Exception e) {
            log.warn("Error during startup cleanup of invalid songs: {}", e.getMessage());
        }
    }


    public UUID newSong() {
        Song song = new Song();
        UUID uuid = UUID.randomUUID();
        song.setId(uuid);
        songService.save(song);
        return uuid;

    }

    public String songLoaded(LoadedSong loadedSong) {
        songService.updateSongPath(loadedSong.getUuid(),loadedSong.getFilePath());
        String user = userService.getUserBySongID(loadedSong.getUuid()).getUserName();
        queueService.addToQueue(loadedSong.getUuid(),user);
        return user;
    }

    public String delete(LoadedSong loadedSong) {
        String user = userService.getUserBySongID(loadedSong.getUuid()).getUserName();

        userService.deleteUserSongBySongId(loadedSong.getUuid());
        songService.delete(loadedSong.getUuid());
        return user;
    }

    public void songFound(RequestSlskdSong requestSlskdSong) {
        songService.updateSongName(requestSlskdSong);



    }

    public void addToUser(String username, UUID songId) {
        User user = userService.getUserByName(username);
        List<UserSong> songs = user.getSongs();
        // Ensure lazy-loaded collection is initialized
        if (songs == null) {
            songs = new java.util.ArrayList<>();
            user.setSongs(songs);
        }
        Song song = songService.findByUUID(songId);
        songs.add(song.toUserSong(user));

        userService.save(user);

    }
}
