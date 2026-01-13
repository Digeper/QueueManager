package org.muzika.queuemanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Queue response containing list of songs")
public class QueueResponse {
    @Schema(description = "List of songs in the queue", required = true)
    private List<SongDTO> songs;
}

