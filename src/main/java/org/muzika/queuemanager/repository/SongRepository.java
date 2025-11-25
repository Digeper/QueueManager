package org.muzika.queuemanager.repository;


import org.muzika.queuemanager.entities.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SongRepository extends JpaRepository<Song, UUID> {

    @Modifying
    @Query("UPDATE Song u SET u.url = :url WHERE u.id = :id")
    void updateSong(@Param("id") UUID id, @Param("url") String url);


}
