package org.muzika.queuemanager.entities;


import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    private String userName;

    @OneToOne(fetch = FetchType.LAZY)
    private Queue userQueue;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserSong> songs;


}
