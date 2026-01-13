package org.muzika.queuemanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Response indicating if a song is liked by the user")
public class SongLikedResponse {
    @Schema(description = "Whether the song is liked", example = "true", required = true)
    private boolean liked;

    public SongLikedResponse(boolean liked) {
        this.liked = liked;
    }
}

