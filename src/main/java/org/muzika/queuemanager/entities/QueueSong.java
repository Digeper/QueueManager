package org.muzika.queuemanager.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.UUID;

@Entity
@Data
@Table(
    name = "queue_songs",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_queue_user_song",
            columnNames = {"queue_user_uuid", "songs_id"}
        )
    }
)
public class QueueSong {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "queue_user_uuid", nullable = false)
    private UUID queueUserUuid;

    @Column(name = "songs_id", nullable = false)
    private UUID songsId;

    @Column(name = "queue_uuid", nullable = false)
    private UUID queueUuid;

    @Column(name = "position", nullable = false)
    private Integer position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_user_uuid", referencedColumnName = "user_uuid", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Queue queue;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "songs_id", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Song song;

}

