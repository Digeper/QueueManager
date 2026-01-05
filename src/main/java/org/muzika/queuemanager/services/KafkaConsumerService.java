package org.muzika.queuemanager.services;


import lombok.extern.slf4j.Slf4j;
import org.muzika.queuemanager.kafkaMassages.LoadedSong;
import org.muzika.queuemanager.kafkaMassages.RequestSlskdSong;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class KafkaConsumerService {



    QueueManagerService  queueManagerService;
    QueueCheckerService queueCheckerService;

    public KafkaConsumerService(QueueManagerService queueManagerService, QueueCheckerService queueCheckerService) {
        this.queueManagerService = queueManagerService;
        this.queueCheckerService = queueCheckerService;
    }


    @KafkaListener(topics = {"loaded-song"} , groupId = "group-id", containerFactory = "loadedSongListenerContainerFactory")
    public void consumeRequestSong(LoadedSong loadedSong) {
        log.info("Received request song: {}",loadedSong);

        if (loadedSong.getStatus() == LoadedSong.Status.COMPLETED){
            queueManagerService.songLoaded(loadedSong);
            // Check and refill queue after a song is successfully loaded
            // Note: Using "admin" as default since Kafka messages don't have user context
            // In the future, this could be enhanced to track which user requested the song
            queueCheckerService.ensureMinimumQueueSize("admin");
        }else {
            queueManagerService.delete(loadedSong);
        }

    }

    @KafkaListener(topics = {"request-slskd-song"} , groupId = "group-id",containerFactory = "songConcurrentKafkaListenerContainerFactory")
    public void consumeRequestSong(RequestSlskdSong requestSlskdSong) {
        log.info("Received request song: {}",requestSlskdSong);

        queueManagerService.songFound(requestSlskdSong);
    }
}
