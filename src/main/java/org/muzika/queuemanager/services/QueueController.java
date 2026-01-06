package org.muzika.queuemanager.services;

import lombok.extern.slf4j.Slf4j;
import org.muzika.queuemanager.dto.AddSongToQueueRequest;
import org.muzika.queuemanager.dto.QueueResponse;
import org.muzika.queuemanager.dto.SongDTO;
import org.muzika.queuemanager.dto.SongIdRequest;
import org.muzika.queuemanager.entities.Queue;
import org.muzika.queuemanager.entities.Song;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
public class QueueController {

    private final QueueService queueService;
    private final QueueCheckerService queueCheckerService;
    private final SongService songService;
    private final FileStorageService fileStorageService;
    private final UserService userService;

    public QueueController(QueueService queueService, QueueCheckerService queueCheckerService,
                           SongService songService, FileStorageService fileStorageService, UserService userService) {
        this.queueService = queueService;
        this.queueCheckerService = queueCheckerService;
        this.songService = songService;
        this.fileStorageService = fileStorageService;
        this.userService = userService;
    }

    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        throw new IllegalStateException("User not authenticated");
    }

    @GetMapping("/queue")
    public ResponseEntity<QueueResponse> getQueue() {
        log.debug("Get queue{}", getAuthenticatedUsername());
        try {
            String username = getAuthenticatedUsername();
            Queue queue = queueService.getOrCreateQueue(username);
            if (queue == null) {
                log.error("Queue {} not found", getAuthenticatedUsername());
            }
            
            if (queue == null || queue.getSongs() == null || queue.getSongs().isEmpty()) {
                QueueResponse response = new QueueResponse();
                java.util.concurrent.CompletableFuture<Boolean>  future= CompletableFuture.supplyAsync(()-> {return queueCheckerService.ensureMinimumQueueSize(username);});
                response.setSongs(new ArrayList<>());
                return ResponseEntity.ok(response);
            }
            
            List<SongDTO> songDTOs = new ArrayList<>();
            for (Song song : queue.getSongs()) {
                SongDTO songDTO = convertToDTO(song);
                songDTOs.add(songDTO);
            }
            
            QueueResponse response = new QueueResponse();
            response.setSongs(songDTOs);
            return ResponseEntity.ok(response);
        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/queue")
    public ResponseEntity<?> addSongToQueue(@RequestBody AddSongToQueueRequest request) {
        try {
            String username = getAuthenticatedUsername();
            
            if (request.getSongId() == null) {
                return ResponseEntity.badRequest().body("songId is required");
            }
            
            if (request.getPosition() == null) {
                return ResponseEntity.badRequest().body("position is required");
            }
            
            queueService.addToQueueAtPosition(username, request.getSongId(), request.getPosition());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/queue/check")
    public ResponseEntity<?> checkAndRefillQueue() {
        try {
            String username = getAuthenticatedUsername();
            boolean success = queueCheckerService.ensureMinimumQueueSize(username);
            if (success) {
                return ResponseEntity.ok().body("Queue check completed successfully");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Queue check completed with some errors. Check logs for details.");
            }
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error during queue check: " + e.getMessage());
        }
    }

    @PostMapping("/queue/skipped")
    public ResponseEntity<?> markSongAsSkipped(@RequestBody SongIdRequest request) {
        try {
            String username = getAuthenticatedUsername();
            
            if (request.getSongId() == null) {
                return ResponseEntity.badRequest().body("songId is required");
            }
            
            UUID songId = request.getSongId();
            
            // Mark song as skipped in UserSong
            userService.markSongAsSkipped(username, songId);
            
            // Remove song from queue
            queueService.removeSongFromQueue(username, songId);
            
            // Refill queue to ensure minimum size
            queueCheckerService.ensureMinimumQueueSize(username);
            
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error marking song as skipped: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/queue/finished")
    public ResponseEntity<?> markSongAsFinished(@RequestBody SongIdRequest request) {
        try {
            String username = getAuthenticatedUsername();
            
            if (request.getSongId() == null) {
                return ResponseEntity.badRequest().body("songId is required");
            }
            
            UUID songId = request.getSongId();
            
            // Increment listen count in UserSong
            userService.incrementSongListenCount(username, songId);
            
            // Remove song from queue
            queueService.removeSongFromQueue(username, songId);
            
            // Refill queue to ensure minimum size
            queueCheckerService.ensureMinimumQueueSize(username);
            
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error marking song as finished: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/songs/{id}")
    public ResponseEntity<Resource> getSong(@PathVariable UUID id) {
        try {
            // Authentication check
            String username = getAuthenticatedUsername();
            log.debug("User {} requesting song {}", username, id);

            // Retrieve song from service
            Song song;
            try {
                song = songService.findByUUID(id);
            } catch (RuntimeException e) {
                log.warn("Song not found: {}", id);
                return ResponseEntity.notFound().build();
            }

            // Get file path from song's url field
            String filePath = song.getUrl();
            if (filePath == null || filePath.trim().isEmpty()) {
                log.warn("Song {} has no file path", id);
                return ResponseEntity.notFound().build();
            }

            // Retrieve file from storage
            Resource resource;
            try {
                resource = fileStorageService.getFile(filePath);
            } catch (IOException e) {
                log.error("Error retrieving file for song {}: {}", id, e.getMessage());
                return ResponseEntity.notFound().build();
            } catch (IllegalArgumentException e) {
                log.error("Invalid file path for song {}: {}", id, e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // Determine content type
            MediaType mediaType = determineContentType(filePath);
            
            // Extract filename for Content-Disposition header
            String filename = extractFilename(filePath);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentLength(resource.contentLength());
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

            log.debug("Successfully serving song {} as file {}", id, filePath);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (IllegalStateException e) {
            log.warn("Unauthorized access attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Unexpected error retrieving song {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Determines the content type based on file extension.
     */
    private MediaType determineContentType(String filePath) {
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".mp3")) {
            return MediaType.parseMediaType("audio/mpeg");
        } else if (lowerPath.endsWith(".flac")) {
            return MediaType.parseMediaType("audio/flac");
        } else if (lowerPath.endsWith(".wav")) {
            return MediaType.parseMediaType("audio/wav");
        } else if (lowerPath.endsWith(".aiff") || lowerPath.endsWith(".aif")) {
            return MediaType.parseMediaType("audio/aiff");
        } else if (lowerPath.endsWith(".m4a")) {
            return MediaType.parseMediaType("audio/mp4");
        } else if (lowerPath.endsWith(".ogg")) {
            return MediaType.parseMediaType("audio/ogg");
        } else if (lowerPath.endsWith(".wma")) {
            return MediaType.parseMediaType("audio/x-ms-wma");
        } else {
            // Default to binary if unknown
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * Extracts the filename from a file path.
     */
    private String extractFilename(String filePath) {
        Path path = Paths.get(filePath);
        return path.getFileName().toString();
    }

    private SongDTO convertToDTO(Song song) {
        SongDTO dto = new SongDTO();
        dto.setId(song.getId());
        dto.setTitle(song.getTitle());
        dto.setArtist(song.getArtist());
        dto.setAlbum(song.getAlbum());
        dto.setGenre(song.getGenre());
        dto.setDuration(song.getDuration());
        dto.setUrl(song.getUrl());
        return dto;
    }
}
