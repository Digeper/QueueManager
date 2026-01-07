package org.muzika.queuemanager.services;

import jakarta.transaction.Transactional;
import org.muzika.queuemanager.entities.Queue;
import org.muzika.queuemanager.entities.Song;
import org.muzika.queuemanager.entities.User;
import org.muzika.queuemanager.entities.UserSong;
import org.muzika.queuemanager.entities.UserSongId;
import org.muzika.queuemanager.kafkaMassages.LikedSongEvent;
import org.muzika.queuemanager.kafkaMassages.UnlikedSongEvent;
import org.muzika.queuemanager.repository.QueueRepository;
import org.muzika.queuemanager.repository.QueueSongRepository;
import org.muzika.queuemanager.repository.UserRepository;
import org.muzika.queuemanager.repository.UserSongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class UserService {


    private final UserRepository userRepository;
    private final QueueRepository queueRepository;
    private final UserSongRepository userSongRepository;

    private final SongService songService;
    private final KafkaProducerService kafkaProducerService;

    @Autowired
    public UserService(UserRepository userRepository, QueueRepository queueRepository, UserSongRepository userSongRepository, SongService songService, KafkaProducerService kafkaProducerService) {

        this.userRepository = userRepository;
        this.queueRepository = queueRepository;
        this.userSongRepository = userSongRepository;
        this.songService = songService;
        this.kafkaProducerService = kafkaProducerService;
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


        User user = new User();
        user.setUserId(userId);
        user.setUserName(username);
        user = userRepository.save(user);

        Queue queue = new Queue();
        queue.setUser(user);
        queue.setUuid(user.getUuid());
        queue = queueRepository.save(queue);


        user.setUserQueue(queue);
        userRepository.save(user);


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

    public void markSongAsLiked(String username, UUID songId) {
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
            userSong.setLiked(true);
            userSongRepository.save(userSong);
        } else {
            // UserSong doesn't exist, create it with liked=true
            Song song = songService.findByUUID(songId);
            if (song == null) {
                throw new IllegalArgumentException("Song not found: " + songId);
            }
            UserSong userSong = song.toUserSong(user);
            userSong.setLiked(true);
            userSongRepository.save(userSong);
        }

        // Send Kafka event - use userId (auth userId) not uuid (auto-generated PK)
        UUID authUserId = user.getUserId() != null ? user.getUserId() : user.getUuid();
        LikedSongEvent event = new LikedSongEvent(authUserId, username, songId);
        kafkaProducerService.sendLikedSongEvent("liked", authUserId, event);
    }

    public void markSongAsUnliked(String username, UUID songId) {
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
            userSong.setLiked(false);
            userSongRepository.save(userSong);
        } else {
            // UserSong doesn't exist, create it with liked=false
            Song song = songService.findByUUID(songId);
            if (song == null) {
                throw new IllegalArgumentException("Song not found: " + songId);
            }
            UserSong userSong = song.toUserSong(user);
            userSong.setLiked(false);
            userSongRepository.save(userSong);
        }

        // Send Kafka event - use userId (auth userId) not uuid (auto-generated PK)
        UUID authUserId = user.getUserId() != null ? user.getUserId() : user.getUuid();
        UnlikedSongEvent event = new UnlikedSongEvent(authUserId, username, songId);
        kafkaProducerService.sendUnlikedSongEvent("unliked", authUserId, event);
    }

    public boolean isSongLiked(String username, UUID songId) {
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
            return userSong.getLiked() != null && userSong.getLiked();
        } else {
            // UserSong doesn't exist, default to false
            return false;
        }
    }

    public void loadInitalQueue(UUID userId, String username, List<Song> songs) {
        User user = getUserByName(username);
        ArrayList<UserSong> userSongs = new ArrayList<>();
        for (Song song : songs) {
            userSongs.add(song.toUserSong(user));

        }
        user.setSongs(userSongs);
        userRepository.save(user);


    }

    public List<Song> getAllUserSongs(String username) {
        User user = userRepository.findByUserName(username);
        List<UserSong> userSongs= user.getSongs();
        ArrayList<Song> songs = new ArrayList<>();
        for (UserSong userSong : userSongs) {
            songs.add(userSong.getSong());
        }
        return songs;
    }
}
