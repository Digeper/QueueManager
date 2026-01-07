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
@ToString(exclude = {"user", "queueSongs"})
@EqualsAndHashCode(exclude = {"user", "queueSongs"})
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

    @OneToMany(mappedBy = "queue", cascade={CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE, CascadeType.REMOVE},
            fetch=FetchType.EAGER)
    @OrderBy("position ASC")
    private List<QueueSong> queueSongs = new ArrayList<>();

}
