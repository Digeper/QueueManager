package org.muzika.queuemanager.dto;

import lombok.Data;

import java.util.List;

@Data
public class QueueResponse {
    private List<SongDTO> songs;
}

