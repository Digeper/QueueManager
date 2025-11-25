package org.muzika.queuemanager.services;


import jakarta.annotation.PostConstruct;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.entities.User;
import org.muzika.queuemanager.kafkaMassages.LoadedSong;
import org.muzika.queuemanager.kafkaMassages.RequestSlskdSong;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QueueManagerService {

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

    }


    public UUID newSong() {
        Song song = new Song();
        UUID uuid = UUID.randomUUID();
        song.setId(uuid);
        songService.save(song);
        return uuid;

    }

    public void songLoaded(LoadedSong loadedSong) {
        songService.updateSongPath(loadedSong.getUuid(),loadedSong.getFilePath());
        Song song = songService.findByUUID(loadedSong.getUuid());
        queueService.addToQueue("admin",song);
    }

    public void delete(LoadedSong loadedSong) {
        songService.delete(loadedSong.getUuid());
    }

    public void songFound(RequestSlskdSong requestSlskdSong) {
        songService.updateSongName(requestSlskdSong);



    }
}
