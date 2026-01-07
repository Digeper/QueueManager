package org.muzika.queuemanager.services;


import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.muzika.queuemanager.entities.Queue;
import org.muzika.queuemanager.entities.QueueSong;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.entities.User;
import org.muzika.queuemanager.repository.QueueRepository;
import org.muzika.queuemanager.repository.QueueSongRepository;
import org.muzika.queuemanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class QueueService {

    @Autowired
    private QueueRepository queueRepository;
    
    @Autowired
    private QueueSongRepository queueSongRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SongService songService;
    @Autowired
    private UserRepository userRepository;


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
            // Initialize lazy-loaded queueSongs collection within transaction
            // Access the collection to force Hibernate to load it
            List<QueueSong> queueSongs = queue.getQueueSongs();
            if (queueSongs != null) {
                queueSongs.size(); // Force initialization of lazy collection
            }
            return queue;
        } catch (Exception e) {
            throw e;

        }
    }

    public void addToQueue(UUID uuid,String username) {
        Queue queue =  userRepository.findByUserName(username).getUserQueue();


        Song song = songService.findSongById(uuid);
        
        // Get current queue songs to determine next position
        List<QueueSong> queueSongs = queue.getQueueSongs();
        if (queueSongs == null) {
            queueSongs = new ArrayList<>();
            queue.setQueueSongs(queueSongs);
        }
        
        // Check if song already exists in this queue
        boolean alreadyExists = queueSongs.stream()
            .anyMatch(qs -> qs.getSongsId().equals(song.getId()));
        if (alreadyExists) {
            log.warn("Song {} already exists in queue for user {}, skipping", song.getId(), username);
            return;
        }
        
        // Create new QueueSong entity for the song
        QueueSong queueSong = new QueueSong();
        queueSong.setQueueUserUuid(queue.getUserUuid());
        queueSong.setSongsId(song.getId());
        queueSong.setQueueUuid(queue.getUuid());
        queueSong.setPosition(queueSongs.size()); // Add to end
        queueSong.setQueue(queue);
        queueSong.setSong(song);
        
        queueSongs.add(queueSong);
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
        
        List<QueueSong> queueSongs = queue.getQueueSongs();
        if (queueSongs == null) {
            // Initialize the collection if it's null (lazy loading not triggered)
            queue.setQueueSongs(new ArrayList<>());
            queueSongs = queue.getQueueSongs();
        }
        
        // Validate position: allow 0 to size (for appending at end)
        if (position < 0 || position > queueSongs.size()) {
            throw new IllegalArgumentException("Position " + position + " is out of bounds. Queue size: " + queueSongs.size());
        }
        
        // Create new QueueSong entity
        QueueSong queueSong = new QueueSong();
        queueSong.setQueueUserUuid(queue.getUserUuid());
        queueSong.setSongsId(song.getId());
        queueSong.setQueueUuid(queue.getUuid());
        queueSong.setQueue(queue);
        queueSong.setSong(song);
        
        // Insert at position and update positions for all subsequent items
        queueSongs.add(position, queueSong);
        
        // Update positions for all queue songs to maintain order
        for (int i = 0; i < queueSongs.size(); i++) {
            queueSongs.get(i).setPosition(i);
        }
        
        queueRepository.save(queue);
    }

    public void removeSongFromQueue(String username, UUID songId) {
        Queue queue = getOrCreateQueue(username);
        List<QueueSong> queueSongs = queue.getQueueSongs();
        
        if (queueSongs == null || queueSongs.isEmpty()) {
            // Queue is empty, nothing to remove
            return;
        }
        
        // Find and remove the first QueueSong with matching songId (backward compatibility)
        QueueSong toRemove = null;
        for (QueueSong queueSong : queueSongs) {
            if (queueSong.getSongsId().equals(songId)) {
                toRemove = queueSong;
                break;
            }
        }
        
        if (toRemove != null) {
            queueSongs.remove(toRemove);
            queueSongRepository.delete(toRemove);
            
            // Update positions for remaining items to maintain order
            for (int i = 0; i < queueSongs.size(); i++) {
                queueSongs.get(i).setPosition(i);
            }
            
            queueRepository.save(queue);
        }
    }

    /**
     * Remove a specific queue entry by its unique ID.
     * This allows removing a specific instance when the same song appears multiple times in the queue.
     * 
     * @param username The username of the queue owner
     * @param queueEntryId The unique ID of the queue entry to remove
     * @throws IllegalArgumentException if the queue entry is not found
     */
    public void removeQueueEntry(String username, UUID queueEntryId) {
        Queue queue = getOrCreateQueue(username);
        List<QueueSong> queueSongs = queue.getQueueSongs();
        
        if (queueSongs == null || queueSongs.isEmpty()) {
            throw new IllegalArgumentException("Queue is empty");
        }
        
        // Find the specific queue entry by ID
        QueueSong toRemove = null;
        for (QueueSong queueSong : queueSongs) {
            if (queueSong.getId() != null && queueSong.getId().equals(queueEntryId)) {
                toRemove = queueSong;
                break;
            }
        }
        
        if (toRemove == null) {
            throw new IllegalArgumentException("Queue entry with ID " + queueEntryId + " not found in queue");
        }
        
        queueSongs.remove(toRemove);
        queueSongRepository.delete(toRemove);
        
        // Update positions for remaining items to maintain order
        for (int i = 0; i < queueSongs.size(); i++) {
            queueSongs.get(i).setPosition(i);
        }
        
        queueRepository.save(queue);
    }

    public void removeSongFromAllQueues(UUID songId) {
        List<User> allUsers = userService.getAllUsers();
        for (User user : allUsers) {
            if (user.getUserName() != null) {
                Queue queue = getOrCreateQueue(user.getUserName());
                List<QueueSong> queueSongs = queue.getQueueSongs();
                
                if (queueSongs != null && !queueSongs.isEmpty()) {
                    // Remove all instances of this song from this user's queue
                    List<QueueSong> toRemove = new ArrayList<>();
                    for (QueueSong queueSong : queueSongs) {
                        if (queueSong.getSongsId().equals(songId)) {
                            toRemove.add(queueSong);
                        }
                    }
                    
                    for (QueueSong queueSong : toRemove) {
                        queueSongs.remove(queueSong);
                        queueSongRepository.delete(queueSong);
                    }
                    
                    // Update positions for remaining items
                    for (int i = 0; i < queueSongs.size(); i++) {
                        queueSongs.get(i).setPosition(i);
                    }
                    
                    if (!toRemove.isEmpty()) {
                        queueRepository.save(queue);
                    }
                }
            }
        }
    }

}

