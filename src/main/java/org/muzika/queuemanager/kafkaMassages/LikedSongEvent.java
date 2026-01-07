package org.muzika.queuemanager.kafkaMassages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikedSongEvent {
    private UUID userId;
    private String username;
    private UUID songId;
}

