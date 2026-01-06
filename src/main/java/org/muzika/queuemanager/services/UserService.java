package org.muzika.queuemanager.services;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.muzika.queuemanager.entities.Queue;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.entities.User;
import org.muzika.queuemanager.entities.UserSong;
import org.muzika.queuemanager.entities.UserSongId;
import org.muzika.queuemanager.repository.QueueRepository;
import org.muzika.queuemanager.repository.SongRepository;
import org.muzika.queuemanager.repository.UserRepository;
import org.muzika.queuemanager.repository.UserSongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {


    private final UserRepository userRepository;
    private final QueueRepository queueRepository;
    private final EntityManager entityManager;
    private final UserSongRepository userSongRepository;
    private final SongService songService;

    @Autowired
    public UserService(UserRepository userRepository, QueueRepository queueRepository, EntityManager entityManager, UserSongRepository userSongRepository, SongService songService) {
        this.userRepository = userRepository;
        this.queueRepository = queueRepository;
        this.entityManager = entityManager;
        this.userSongRepository = userSongRepository;
        this.songService = songService;

    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    public long count() {
        return userRepository.count();
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public User getUserByName(String username) {
        return userRepository.findByUserName(username);
    }


    @Transactional
    public User createUserFromEvent(UUID userId, String username) {
        // Check if user already exists
        if (userRepository.existsById(userId)) {
            return userRepository.findById(userId).orElseThrow();
        }

        // Create new user with specific UUID and username
        // Set uuid (primary key) directly
        User user = new User();
        user.setUuid(userId);  // Set the primary key UUID
        user.setUserName(username);
        user = entityManager.merge(user);  // Use merge to handle pre-set UUID
        entityManager.flush();

        // Create and associate Queue
        // IMPORTANT: Don't set songs here - let it be null to avoid PersistentBag error
        // It will be initialized when first accessed (as QueueService does)
        Queue queue = new Queue();
        queue.setUser(user);
        // Set uuid to match userUuid (will be set by @MapsId, but ensure uuid is also set)
        queue.setUuid(user.getUuid());
        // Don't set songs - leave it null, Hibernate will handle it
        queue = queueRepository.save(queue);
        user.setUserQueue(queue);
        user = userRepository.save(user);

        return user;
    }

    public UUID getUserIdByUsername(String username) {
        return userRepository.findByUserName(username).getUuid();
    }


    public void deleteUserSongBySongId(UUID uuid) {
        UserSong userSong = userSongRepository.findBySongId(uuid);
        userSongRepository.delete(userSong);
    }

    public void deleteAllUserSongsBySongId(UUID songId) {
        List<UserSong> userSongs = userSongRepository.findAllBySongId(songId);
        userSongRepository.deleteAll(userSongs);
    }

    public User getUserBySongID(UUID uuid) {
        return userSongRepository.findBySongId(uuid).getUser();
    }

    public Song findSongById(UUID uuid) {
        return userSongRepository.findBySongId(uuid).getSong();
    }

    public void markSongAsSkipped(String username, UUID songId) {
        User user = getUserByName(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }

        UserSongId userSongId = new UserSongId();
        userSongId.setUserId(user.getUuid());
        userSongId.setSongId(songId);

        Optional<UserSong> userSongOpt = userSongRepository.findById(userSongId);
        if (userSongOpt.isPresent()) {
            UserSong userSong = userSongOpt.get();
            userSong.setSkipped(true);
            userSongRepository.save(userSong);
        } else {
            // UserSong doesn't exist, create it with skipped=true
            Song song = songService.findByUUID(songId);
            if (song == null) {
                throw new IllegalArgumentException("Song not found: " + songId);
            }
            UserSong userSong = song.toUserSong(user);
            userSong.setSkipped(true);
            userSongRepository.save(userSong);
        }
    }

    public void incrementSongListenCount(String username, UUID songId) {
        User user = getUserByName(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }

        UserSongId userSongId = new UserSongId();
        userSongId.setUserId(user.getUuid());
        userSongId.setSongId(songId);

        Optional<UserSong> userSongOpt = userSongRepository.findById(userSongId);
        if (userSongOpt.isPresent()) {
            UserSong userSong = userSongOpt.get();
            // Increment listen count
            int currentCount = userSong.getListenCount() != null ? userSong.getListenCount() : 0;
            userSong.setListenCount(currentCount + 1);
            
            // Update timestamps
            LocalDateTime now = LocalDateTime.now();
            if (userSong.getFirstListen() == null) {
                userSong.setFirstListen(now);
            }
            userSong.setLastListen(now);
            
            userSongRepository.save(userSong);
        } else {
            // UserSong doesn't exist, create it with listen count 1
            Song song = songService.findByUUID(songId);
            if (song == null) {
                throw new IllegalArgumentException("Song not found: " + songId);
            }
            UserSong userSong = song.toUserSong(user);
            userSong.setListenCount(1);
            LocalDateTime now = LocalDateTime.now();
            userSong.setFirstListen(now);
            userSong.setLastListen(now);
            userSongRepository.save(userSong);
        }
    }
}
