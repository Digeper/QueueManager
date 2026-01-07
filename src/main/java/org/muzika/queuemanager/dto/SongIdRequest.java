package org.muzika.queuemanager.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class SongIdRequest {
    private UUID songId;
    // Optional queue entry ID - if provided, removes the specific queue entry instead of first match
    private UUID queueEntryId;
}

