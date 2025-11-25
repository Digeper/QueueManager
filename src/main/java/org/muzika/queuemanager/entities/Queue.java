package org.muzika.queuemanager.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayDeque;
import java.util.UUID;

@Data
@Entity

public class Queue {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @OneToOne(fetch = FetchType.LAZY)
    private User user;


    @OneToMany(cascade={CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE},
            fetch=FetchType.LAZY)
    private ArrayDeque<Song> songs;


}
