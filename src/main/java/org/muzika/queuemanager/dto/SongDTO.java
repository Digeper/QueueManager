package org.muzika.queuemanager.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class SongDTO {
    private UUID id;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private Long duration;
    private String url;
}

