package org.muzika.queuemanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Request with song ID and optional queue entry ID")
public class SongIdRequest {
    @Schema(description = "Song UUID", example = "770e8400-e29b-41d4-a716-446655440002", required = true)
    private UUID songId;
    
    @Schema(description = "Optional queue entry ID - if provided, removes the specific queue entry instead of first match", example = "880e8400-e29b-41d4-a716-446655440003")
    private UUID queueEntryId;
}

