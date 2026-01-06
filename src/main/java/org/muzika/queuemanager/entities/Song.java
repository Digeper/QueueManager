package org.muzika.queuemanager.entities;


import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
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

    @OneToMany(mappedBy = "song", cascade = CascadeType.MERGE)
    @ToString.Exclude  // Exclude from toString
    @EqualsAndHashCode.Exclude  // Exclude from equals/hashCode
    private List<UserSong> userSongs = new ArrayList<>();

    // Custom toString() without circular reference
    @Override
    public String toString() {
        return "Song{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", genre='" + genre + '\'' +
                ", duration=" + duration +
                ", url='" + url + '\'' +
                '}';
    }


    public UserSong toUserSong(User user) {
        UserSong userSong = new UserSong();
        userSong.setSong(this);
        userSong.setUser(user);
        userSong.setSongId(this.getId());
        userSong.setUserId(user.getUuid());
        userSong.setListenCount(0);
        return userSong;
    }
}
