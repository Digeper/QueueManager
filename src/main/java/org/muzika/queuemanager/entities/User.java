package org.muzika.queuemanager.entities;


import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "users")
@NoArgsConstructor
@ToString(exclude = {"userQueue", "songs"})
@EqualsAndHashCode(exclude = {"userQueue", "songs"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    private String userName;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "user")
    private Queue userQueue;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserSong> songs;


    public User(UUID userI, String username) {
        uuid = userI;
        userName = username;
    }
}

