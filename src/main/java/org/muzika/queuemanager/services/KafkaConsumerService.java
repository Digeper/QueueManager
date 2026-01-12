package org.muzika.queuemanager.services;


import lombok.extern.slf4j.Slf4j;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.entities.User;
import org.muzika.queuemanager.kafkaMassages.LoadedSong;
import org.muzika.queuemanager.kafkaMassages.RequestSlskdSong;
import org.muzika.queuemanager.kafkaMassages.UserCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class KafkaConsumerService {



    QueueManagerService  queueManagerService;
    QueueCheckerService queueCheckerService;
    UserService userService;
    SongService songService;
    QueueService queueService;

    public KafkaConsumerService(QueueManagerService queueManagerService, QueueCheckerService queueCheckerService, UserService userService, SongService songService, QueueService queueService) {
        this.queueManagerService = queueManagerService;
        this.queueCheckerService = queueCheckerService;
        this.userService = userService;
        this.songService = songService;
        this.queueService =  queueService;
    }


    @KafkaListener(topics = {"loaded-song"} , groupId = "group-id", containerFactory = "loadedSongListenerContainerFactory")
    public void consumeRequestSong(LoadedSong loadedSong) {
        log.info("Received request song: {}",loadedSong);
        try{
            String username;
            if (loadedSong.getStatus() == LoadedSong.Status.COMPLETED){
                username = queueManagerService.songLoaded(loadedSong);

                // Check and refill queue after a song is successfully loaded
                // Note: Using "admin" as default since Kafka messages don't have user context
                // In the future, this could be enhanced to track which user requested the song
            }else {

                username = queueManagerService.delete(loadedSong);
            }
            java.util.concurrent.CompletableFuture<Boolean>  future= CompletableFuture.supplyAsync(()-> {return queueCheckerService.ensureMinimumQueueSize(username,10,1);});
        }catch (Exception e){
            log.info("song might be missing {} {}",e,loadedSong.getUuid());
        }


    }

    @KafkaListener(topics = {"request-slskd-song"} , groupId = "group-id",containerFactory = "songConcurrentKafkaListenerContainerFactory")
    public void consumeRequestSong(RequestSlskdSong requestSlskdSong) {
        log.info("Received request song: {}",requestSlskdSong);
        try {
            queueManagerService.songFound(requestSlskdSong);
        } catch (Exception e) {
            log.error("song might me missing {}{}",e,requestSlskdSong.getId());
        }

    }

    @KafkaListener(topics = {"user-created"}, groupId = "queue-manager-group", containerFactory = "userCreatedListenerContainerFactory")
    public void consumeUserCreatedEvent(UserCreatedEvent event) {
        log.info("Received user created event: userId={}, username={}", event.getUserId(), event.getUsername());
        try {
            // Check if user already exists
            userService.getUserIdByUsername(event.getUsername());
            log.info("User already exists in QueueManager: username={}", event.getUsername());
        } catch (Exception e1) {
            // User doesn't exist, create it
            try {
                userService.createUserFromEvent(event.getUserId(), event.getUsername());
                log.info("Successfully created user in QueueManager: userId={}, username={}", event.getUserId(), event.getUsername());
                
                // Initialize queue with random songs
                List<Song> songs = songService.getRandomSongsWithUrl(10);
                try {
                    userService.loadInitalQueue(event.getUserId(), event.getUsername(), songs);
                    log.info("Successfully loaded initial songs for user: username={}, songCount={}", event.getUsername(), songs.size());
                } catch (Exception e) {
                    log.error("Failed to load initial queue for user: username={}, error={}", event.getUsername(), e.getMessage(), e);
                }
                
                // Add songs to queue
                songs = userService.getAllUserSongs(event.getUsername());
                try {
                    for (Song song : songs) {
                        queueService.addToQueue(song.getId(), event.getUsername());
                    }
                    log.info("Successfully added songs to queue for user: username={}, songCount={}", event.getUsername(), songs.size());
                } catch (Exception e) {
                    log.error("Failed to add songs to queue for user: username={}, error={}", event.getUsername(), e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error("Failed to create user from event: userId={}, username={}, error={}",
                        event.getUserId(), event.getUsername(), e.getMessage(), e);
            }
        }
    }
}
