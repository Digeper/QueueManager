package org.muzika.queuemanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Song information")
public class SongDTO {
    @Schema(description = "Song UUID", example = "770e8400-e29b-41d4-a716-446655440002", required = true)
    private UUID id;
    
    @Schema(description = "Song title", example = "Bohemian Rhapsody", required = true)
    private String title;
    
    @Schema(description = "Artist name", example = "Queen")
    private String artist;
    
    @Schema(description = "Album name", example = "A Night at the Opera")
    private String album;
    
    @Schema(description = "Genre", example = "Rock")
    private String genre;
    
    @Schema(description = "Duration in milliseconds", example = "355000")
    private Long duration;
    
    @Schema(description = "File path/URL to the song file")
    private String url;
    
    @Schema(description = "Optional queue entry ID - only present when returned as part of a queue", example = "880e8400-e29b-41d4-a716-446655440003")
    private UUID queueEntryId;
}

