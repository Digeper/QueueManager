package org.muzika.queuemanager.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class AddSongToQueueRequest {
    private UUID songId;
    private Integer position;
}

