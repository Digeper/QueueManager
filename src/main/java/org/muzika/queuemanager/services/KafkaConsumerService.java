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

    public KafkaConsumerService(QueueManagerService queueManagerService) {
        this.queueManagerService = queueManagerService;
    }


    @KafkaListener(topics = {"loaded-song"} , groupId = "group-id", containerFactory = "loadedSongListenerContainerFactory")
    public void consumeRequestSong(LoadedSong loadedSong) {
        log.info("Received request song: {}",loadedSong);

        if (loadedSong.getStatus() == LoadedSong.Status.COMPLETED){
            queueManagerService.songLoaded(loadedSong);
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
