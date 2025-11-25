package org.muzika.queuemanager.entities;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "user_songs")
@IdClass(UserSongId.class)
public class UserSong {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "song_id")
    private UUID songId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", insertable = false, updatable = false)
    private Song song;

    @Column(name = "listen_count")
    private Integer listenCount  = 0;

    @Column(name = "first_listend_at")
    private LocalDateTime firstListen;

    @Column(name = "last_listend_at")
    private LocalDateTime lastListen;

    @Column(name = "liked")
    private Boolean liked = false;

    @Column(name = "skipped")
    private Boolean skipped= false;




}
