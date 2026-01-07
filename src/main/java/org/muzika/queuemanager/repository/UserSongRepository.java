package org.muzika.queuemanager.repository;


import jakarta.transaction.Transactional;
import org.muzika.queuemanager.entities.UserSong;
import org.muzika.queuemanager.entities.UserSongId;
import org.muzika.queuemanager.services.QueueController;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Transactional
@Repository
public interface UserSongRepository extends JpaRepository<UserSong, UserSongId> {

    UserSong findBySongId(UUID uuid);
    
    List<UserSong> findAllBySongId(UUID songId);

}