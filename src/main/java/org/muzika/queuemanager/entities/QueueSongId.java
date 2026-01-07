package org.muzika.queuemanager.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueueSongId implements Serializable {

    private UUID queueUserUuid;
    private UUID songsId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueueSongId that = (QueueSongId) o;
        return Objects.equals(queueUserUuid, that.queueUserUuid) && Objects.equals(songsId, that.songsId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queueUserUuid, songsId);
    }

}

