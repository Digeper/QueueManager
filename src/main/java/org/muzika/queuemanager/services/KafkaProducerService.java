package org.muzika.queuemanager.services;


import org.muzika.queuemanager.kafkaMassages.LikedSongEvent;
import org.muzika.queuemanager.kafkaMassages.RequestRandomSong;
import org.muzika.queuemanager.kafkaMassages.UnlikedSongEvent;
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

    @Autowired
    KafkaTemplate<UUID, LikedSongEvent> likedSongKafka;

    @Autowired
    KafkaTemplate<UUID, UnlikedSongEvent> unlikedSongKafka;

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

    public void sendLikedSongEvent(String topic, UUID uuid, LikedSongEvent event) {
        var future = likedSongKafka.send(topic, uuid, event);
        future.whenComplete((r, e) -> {
            if (e != null) {
                logger.error("Failed to send liked song event: {}", e.getMessage());
                future.completeExceptionally(e);
            } else {
                logger.info("Sent liked song event: {}", event);
                future.complete(r);
            }
        });
    }

    public void sendUnlikedSongEvent(String topic, UUID uuid, UnlikedSongEvent event) {
        var future = unlikedSongKafka.send(topic, uuid, event);
        future.whenComplete((r, e) -> {
            if (e != null) {
                logger.error("Failed to send unliked song event: {}", e.getMessage());
                future.completeExceptionally(e);
            } else {
                logger.info("Sent unliked song event: {}", event);
                future.complete(r);
            }
        });
    }
}
