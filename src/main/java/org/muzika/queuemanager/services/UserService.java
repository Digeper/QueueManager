package org.muzika.queuemanager.services;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.muzika.queuemanager.entities.Queue;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.entities.User;
import org.muzika.queuemanager.entities.UserSong;
import org.muzika.queuemanager.repository.QueueRepository;
import org.muzika.queuemanager.repository.SongRepository;
import org.muzika.queuemanager.repository.UserRepository;
import org.muzika.queuemanager.repository.UserSongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Autowired
    public UserService(UserRepository userRepository, QueueRepository queueRepository, EntityManager entityManager, UserSongRepository userSongRepository) {
        this.userRepository = userRepository;
        this.queueRepository = queueRepository;
        this.entityManager = entityManager;
        this.userSongRepository = userSongRepository;

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

    public User getUserBySongID(UUID uuid) {
        return userSongRepository.findBySongId(uuid).getUser();
    }

    public Song findSongById(UUID uuid) {
        return userSongRepository.findBySongId(uuid).getSong();
    }
}
