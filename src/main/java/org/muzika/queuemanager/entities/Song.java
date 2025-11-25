package org.muzika.queuemanager.entities;


import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "songs")
public class Song {

    @Id
    private UUID id;

    private String title;
    private String artist;
    private String album;
    private String genre;
    private Long duration;
    private String url;

    @OneToMany(mappedBy = "song",cascade = CascadeType.ALL)
    private List<UserSong> userSongs;


}
