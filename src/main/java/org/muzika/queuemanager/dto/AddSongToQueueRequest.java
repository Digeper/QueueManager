package org.muzika.queuemanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Request to add a song to the queue at a specific position")
public class AddSongToQueueRequest {
    @Schema(description = "Song UUID", example = "770e8400-e29b-41d4-a716-446655440002", required = true)
    private UUID songId;
    
    @Schema(description = "Position in queue (0-based)", example = "0", required = true)
    private Integer position;
}

