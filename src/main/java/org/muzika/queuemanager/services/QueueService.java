package org.muzika.queuemanager.services;


import jakarta.transaction.Transactional;
import org.muzika.queuemanager.entities.Queue;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.entities.User;
import org.muzika.queuemanager.repository.QueueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class QueueService {

    @Autowired
    private QueueRepository queueRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SongService songService;

    private Queue getOrCreateQueue(String username) {
        try {
            Queue queue = queueRepository.getReferenceByUser_UserName(username);
            if (queue.getSongs() == null) {
                queue.setSongs(new ArrayDeque<>());
            }
            return queue;
        } catch (jakarta.persistence.EntityNotFoundException e) {
            // Queue doesn't exist, create it
            User user = userService.getUserByName(username);
            if (user == null) {
                throw new IllegalArgumentException("User not found: " + username);
            }
            Queue queue = new Queue();
            queue.setUser(user);
            queue.setSongs(new ArrayDeque<>());
            queue = queueRepository.save(queue);
            user.setUserQueue(queue);
            userService.save(user);
            return queue;
        }
    }

    public void addToQueue(String username, Song song) {
        Queue queue = getOrCreateQueue(username);
        queue.getSongs().addLast(song);
        queueRepository.save(queue);
    }

    public Queue getQueueByUsername(String username) {
        return getOrCreateQueue(username);
    }

    public void addToQueueAtPosition(String username, UUID songId, int position) {
        Queue queue = getOrCreateQueue(username);
        
        // Validate song exists
        Song song = songService.findByUUID(songId);
        if (song == null) {
            throw new IllegalArgumentException("Song with ID " + songId + " not found");
        }
        
        ArrayDeque<Song> songs = queue.getSongs();
        List<Song> songList = new ArrayList<>(songs);
        
        // Validate position: allow 0 to size (for appending at end)
        if (position < 0 || position > songList.size()) {
            throw new IllegalArgumentException("Position " + position + " is out of bounds. Queue size: " + songList.size());
        }
        
        // Insert at position
        songList.add(position, song);
        
        // Convert back to ArrayDeque
        queue.setSongs(new ArrayDeque<>(songList));
        queueRepository.save(queue);
    }
}
