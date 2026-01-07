package org.muzika.queuemanager.repository;

import jakarta.transaction.Transactional;
import org.muzika.queuemanager.entities.QueueSong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Transactional
@Repository
public interface QueueSongRepository extends JpaRepository<QueueSong, UUID> {

    List<QueueSong> findByQueueUserUuid(UUID queueUserUuid);
    
    List<QueueSong> findBySongsId(UUID songsId);
    
    List<QueueSong> findByQueueUserUuidAndSongsId(UUID queueUserUuid, UUID songsId);
    
    void deleteByQueueUserUuid(UUID queueUserUuid);
    
    void deleteBySongsId(UUID songsId);

}

