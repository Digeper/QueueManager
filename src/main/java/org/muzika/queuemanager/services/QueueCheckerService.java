package org.muzika.queuemanager.services;

import jakarta.transaction.Transactional;
import org.muzika.queuemanager.entities.Queue;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.kafkaMassages.RequestRandomSong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class QueueCheckerService {

    private static final Logger logger = LoggerFactory.getLogger(QueueCheckerService.class);
    private static final int MIN_QUEUE_SIZE = 10;
    private static final String DEFAULT_GENRE = "hisa";

    private final QueueService queueService;
    private final QueueManagerService queueManagerService;
    private final KafkaProducerService kafkaProducerService;

    public QueueCheckerService(QueueService queueService, 
                               QueueManagerService queueManagerService,
                               KafkaProducerService kafkaProducerService) {
        this.queueService = queueService;
        this.queueManagerService = queueManagerService;
        this.kafkaProducerService = kafkaProducerService;
    }

    /**
     * Checks if the queue has at least 10 songs.
     * If not, requests songs from Bandcamp API to fill the queue.
     * 
     * @param username The username to check the queue for
     * @return true if the check completed successfully (queue has enough songs or requests were sent)
     *         false if an error occurred
     */
    public boolean ensureMinimumQueueSize(String username, int i, int i1) {
        try {

            Queue queue = queueService.getQueueByUsername(username);
            // Access lazy-loaded collection within transaction
            List<org.muzika.queuemanager.entities.QueueSong> queueSongs = queue.getQueueSongs();
            int currentSize = (queueSongs == null) ? 0 : queueSongs.size();
            
            logger.info("Current queue size: {}", currentSize);
            
            if (currentSize >= i) {
                logger.info("Queue has {} songs, which meets the minimum requirement of {}", currentSize, MIN_QUEUE_SIZE);
                return true;
            }
            
            int songsNeeded = i - currentSize;
            songsNeeded = Math.min(songsNeeded,i1);
            logger.info("Queue needs {} more songs. Requesting from Bandcamp API...", songsNeeded);


            return requestSongsFromBandcamp(songsNeeded,username);
            
        } catch (Exception e) {
            logger.error("Error while checking queue size: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Requests the specified number of songs from Bandcamp API via Kafka.
     * 
     * @param count Number of songs to request
     * @return true if all requests were sent successfully, false otherwise
     */
    private boolean requestSongsFromBandcamp(int count,String username) {
        boolean allSuccessful = true;
        
        for (int i = 0; i < count; i++) {
            try {
                UUID songId = queueManagerService.newSong();
            // useres song add
                queueManagerService.addToUser(username,songId);
                RequestRandomSong request = new RequestRandomSong(songId, DEFAULT_GENRE);
                
                kafkaProducerService.send("request-random-song", UUID.randomUUID(), request);
                logger.info("Requested song {} of {} from Bandcamp API (songId: {})", i + 1, count, songId);
                
            } catch (Exception e) {
                logger.error("Failed to request song {} of {} from Bandcamp API: {}", 
                           i + 1, count, e.getMessage(), e);
                allSuccessful = false;
                // Continue requesting remaining songs even if one fails
            }
        }
        
        if (allSuccessful) {
            logger.info("Successfully requested {} songs from Bandcamp API", count);
        } else {
            logger.warn("Some song requests failed, but {} songs were requested", count);
        }
        
        return allSuccessful;
    }


}

