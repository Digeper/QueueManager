package org.muzika.queuemanager.services;


import jakarta.transaction.Transactional;
import org.muzika.queuemanager.entities.Queue;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.entities.User;
import org.muzika.queuemanager.repository.QueueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

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


    public Queue getOrCreateQueue(String username) {
        try {
            Queue queue = queueRepository.findByUser_UserName(username);
            if (queue == null) {
                User user = userService.getUserByName(username);
                if (user == null) {
                    throw new IllegalArgumentException("User not found: " + username);
                }
                Queue queue1 = new Queue();
                queue1.setUser(user);
                // Set uuid to match userUuid (will be set by @MapsId, but ensure uuid is also set)
                queue1.setUuid(user.getUuid());
                // Don't set songs - leave it null, Hibernate will handle it
                // Songs will be initialized when first accessed
                queue1 = queueRepository.save(queue1);
                user.setUserQueue(queue1);
                userService.save(user);
                return queue1;
            }
            // Initialize lazy-loaded songs collection within transaction
            // Access the collection to force Hibernate to load it
            List<Song> songs = queue.getSongs();
            if (songs != null) {
                songs.size(); // Force initialization of lazy collection
            }
            return queue;
        } catch (Exception e) {
            throw e;

        }
    }

    public void addToQueue(UUID uuid) {
        String username = userService.getUserBySongID(uuid).getUserName();
        Queue queue = getOrCreateQueue(username);
        // Ensure lazy-loaded collection is initialized
        List<Song> songs = queue.getSongs();
        if (songs == null) {
            songs = new ArrayList<>();
            queue.setSongs(songs);
        }
        songs.add(userService.findSongById(uuid)); // Add to end (like addLast)
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
        
        List<Song> songs = queue.getSongs();
        if (songs == null) {
            // Initialize the collection if it's null (lazy loading not triggered)
            queue.setSongs(new ArrayList<>());
            songs = queue.getSongs();
        }
        
        // Validate position: allow 0 to size (for appending at end)
        if (position < 0 || position > songs.size()) {
            throw new IllegalArgumentException("Position " + position + " is out of bounds. Queue size: " + songs.size());
        }
        
        // Insert at position - List supports direct insertion
        songs.add(position, song);
        queueRepository.save(queue);
    }

}
