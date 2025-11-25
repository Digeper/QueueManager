package org.muzika.queuemanager.services;


import jakarta.transaction.Transactional;
import org.muzika.queuemanager.entities.Queue;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.repository.QueueRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Transactional
public class QueueService {

    private QueueRepository queueRepository;

    public void addToQueue(String username, Song song) {
        Queue queue=  queueRepository.getReferenceByUser_UserName(username);
        queue.getSongs().addLast(song);
        queueRepository.save(queue);

    }
}
