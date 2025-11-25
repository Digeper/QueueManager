package org.muzika.queuemanager.services;


import org.muzika.queuemanager.kafkaMassages.RequestRandomSong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class KafkaProducerService {

    private final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    @Autowired
    KafkaTemplate<UUID, RequestRandomSong> requestRandomSongKafka;

    public void send(String topic, UUID uuid, RequestRandomSong song) {
        var future = requestRandomSongKafka.send(topic, uuid, song);
        future.whenComplete((r, e) -> {
            if (e != null) {
                logger.error(e.getMessage());
                future.completeExceptionally(e);
            }else {
                logger.info(song.toString());
                future.complete(r);
            }
        });

    }
}
