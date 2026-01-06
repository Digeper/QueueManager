package org.muzika.queuemanager.dto;

import lombok.Data;

@Data
public class SongLikedResponse {
    private boolean liked;

    public SongLikedResponse(boolean liked) {
        this.liked = liked;
    }
}

