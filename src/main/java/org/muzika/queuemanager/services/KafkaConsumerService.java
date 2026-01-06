package org.muzika.queuemanager.services;


import lombok.extern.slf4j.Slf4j;
import org.muzika.queuemanager.entities.User;
import org.muzika.queuemanager.kafkaMassages.LoadedSong;
import org.muzika.queuemanager.kafkaMassages.RequestSlskdSong;
import org.muzika.queuemanager.kafkaMassages.UserCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class KafkaConsumerService {



    QueueManagerService  queueManagerService;
    QueueCheckerService queueCheckerService;
    UserService userService;

    public KafkaConsumerService(QueueManagerService queueManagerService, QueueCheckerService queueCheckerService, UserService userService) {
        this.queueManagerService = queueManagerService;
        this.queueCheckerService = queueCheckerService;
        this.userService = userService;
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
            queueCheckerService.ensureMinimumQueueSize(username);
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

    @KafkaListener(topics = {"user-created"}, groupId = "group-id", containerFactory = "userCreatedListenerContainerFactory")
    public void consumeUserCreatedEvent(UserCreatedEvent event) {
        log.info("Received user created event: userId={}, username={}", event.getUserId(), event.getUsername());

        try {
            userService.createUserFromEvent(event.getUserId(), event.getUsername());
            log.info("Successfully created user in QueueManager: userId={}, username={}", event.getUserId(), event.getUsername());
        } catch (Exception e) {
            log.error("Failed to create user from event: userId={}, username={}, error={}", 
                    event.getUserId(), event.getUsername(), e.getMessage(), e);
        }
    }
}
