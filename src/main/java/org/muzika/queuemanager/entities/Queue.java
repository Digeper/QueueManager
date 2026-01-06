package org.muzika.queuemanager.entities;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ToString(exclude = {"user", "songs"})
@EqualsAndHashCode(exclude = {"user", "songs"})
@Entity
@Table(name = "queue")
public class Queue {

    @Id
    @Column(name = "user_uuid")
    private UUID userUuid;

    @Column(name = "uuid", nullable = false)
    private UUID uuid;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_uuid", referencedColumnName = "uuid")
    private User user;
    
    @PrePersist
    @PreUpdate
    @PostLoad
    private void setUuidFromUserUuid() {
        if (userUuid != null) {
            uuid = userUuid;
        }
    }

    @OneToMany(cascade={CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE},
            fetch=FetchType.LAZY)
    @JoinTable(
            name = "queue_songs",
            joinColumns = {
                @JoinColumn(name = "queue_user_uuid", referencedColumnName = "user_uuid"),
                @JoinColumn(name = "queue_uuid", referencedColumnName = "uuid")
            },
            inverseJoinColumns = @JoinColumn(name = "songs_id", referencedColumnName = "id")
    )
    @OrderColumn(name = "position")
    private List<Song> songs = new ArrayList<>();

}
