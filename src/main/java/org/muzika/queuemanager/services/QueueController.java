package org.muzika.queuemanager.services;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.muzika.queuemanager.dto.AddSongToQueueRequest;
import org.muzika.queuemanager.dto.QueueResponse;
import org.muzika.queuemanager.dto.SongDTO;
import org.muzika.queuemanager.dto.SongIdRequest;
import org.muzika.queuemanager.dto.SongLikedResponse;
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
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.PATCH})
@RequestMapping("/api/queue")
@Tag(name = "Queue", description = "Queue management endpoints")
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
    @Operation(
        summary = "Get songs in queue",
        description = "Retrieve all songs in the queue for the authenticated user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Queue retrieved successfully",
            content = @Content(schema = @Schema(implementation = QueueResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized (invalid or missing JWT token)"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<QueueResponse> getQueue() {
        log.debug("Get queue{}", getAuthenticatedUsername());
        try {
            String username = getAuthenticatedUsername();
            Queue queue = userService.getUserByName(username).getUserQueue();
            if (queue == null) {
                log.error("Queue {} not found", getAuthenticatedUsername());
            }
            
            List<org.muzika.queuemanager.entities.QueueSong> queueSongs = queue.getQueueSongs();
            if (queue == null || queueSongs == null || queueSongs.isEmpty()) {
                QueueResponse response = new QueueResponse();
                java.util.concurrent.CompletableFuture<Boolean>  future= CompletableFuture.supplyAsync(()-> {return queueCheckerService.ensureMinimumQueueSize(username,10,10);});
                response.setSongs(new ArrayList<>());
                return ResponseEntity.ok(response);
            }
            
            List<SongDTO> songDTOs = new ArrayList<>();
            for (org.muzika.queuemanager.entities.QueueSong queueSong : queueSongs) {
                Song song = queueSong.getSong();
                if (song != null) {
                    SongDTO songDTO = convertToDTO(song);
                    // Include queue entry ID for tracking specific instances
                    songDTO.setQueueEntryId(queueSong.getId());
                    songDTOs.add(songDTO);
                }
            }
            
            QueueResponse response = new QueueResponse();
            response.setSongs(songDTOs);
            java.util.concurrent.CompletableFuture<Boolean>  future= CompletableFuture.supplyAsync(()-> {return queueCheckerService.ensureMinimumQueueSize(username,10,10);});
            return ResponseEntity.ok(response);
        } catch (Exception e) {

            log.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/queue")
    @Operation(
        summary = "Add song to queue",
        description = "Add a song to the queue at a specific position"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Song added to queue successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request (missing required fields or invalid position)"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized (invalid or missing JWT token)"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<?> addSongToQueue(
        @Parameter(description = "Song ID and position to add to queue", required = true)
        @RequestBody AddSongToQueueRequest request) {
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
    @Operation(
        summary = "Check and refill queue",
        description = "Manually trigger queue check and refill to ensure minimum queue size"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Queue check completed successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized (invalid or missing JWT token)"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<?> checkAndRefillQueue() {
        try {
            String username = getAuthenticatedUsername();
            boolean success = queueCheckerService.ensureMinimumQueueSize(username,10,10);
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
    @Operation(
        summary = "Mark song as skipped",
        description = "Mark a song as skipped and remove it from the queue, then refill queue"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Song marked as skipped successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request (missing songId)"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized (invalid or missing JWT token)"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<?> markSongAsSkipped(
        @Parameter(description = "Song ID and optional queue entry ID", required = true)
        @RequestBody SongIdRequest request) {
        try {
            String username = getAuthenticatedUsername();
            
            if (request.getSongId() == null) {
                return ResponseEntity.badRequest().body("songId is required");
            }
            
            UUID songId = request.getSongId();
            
            // Mark song as skipped in UserSong
            userService.markSongAsSkipped(username, songId);
            
            // Remove song from queue - use queueEntryId if provided, otherwise remove first match
            if (request.getQueueEntryId() != null) {
                queueService.removeQueueEntry(username, request.getQueueEntryId());
            } else {
                queueService.removeSongFromQueue(username, songId);
            }
            
            // Refill queue to ensure minimum size
            java.util.concurrent.CompletableFuture<Boolean>  future= CompletableFuture.supplyAsync(()-> {return queueCheckerService.ensureMinimumQueueSize(username,10,10);});
            
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
    @Operation(
        summary = "Mark song as finished",
        description = "Mark a song as finished, increment listen count, remove from queue, and refill queue"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Song marked as finished successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request (missing songId)"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized (invalid or missing JWT token)"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<?> markSongAsFinished(
        @Parameter(description = "Song ID and optional queue entry ID", required = true)
        @RequestBody SongIdRequest request) {
        try {
            String username = getAuthenticatedUsername();
            
            if (request.getSongId() == null) {
                return ResponseEntity.badRequest().body("songId is required");
            }
            
            UUID songId = request.getSongId();
            
            // Increment listen count in UserSong
            userService.incrementSongListenCount(username, songId);
            
            // Remove song from queue - use queueEntryId if provided, otherwise remove first match
            if (request.getQueueEntryId() != null) {
                queueService.removeQueueEntry(username, request.getQueueEntryId());
            } else {
                queueService.removeSongFromQueue(username, songId);
            }
            
            // Refill queue to ensure minimum size
            java.util.concurrent.CompletableFuture<Boolean>  future= CompletableFuture.supplyAsync(()-> {return queueCheckerService.ensureMinimumQueueSize(username,10,10);});


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
    @Operation(
        summary = "Get song file",
        description = "Download the audio file for a song by ID. Returns the audio file with appropriate content type."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Song file retrieved successfully",
            content = @Content(mediaType = "audio/mpeg")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized (invalid or missing JWT token)"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Song not found"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<Resource> getSong(
        @Parameter(description = "Song UUID", required = true, example = "770e8400-e29b-41d4-a716-446655440002")
        @PathVariable UUID id) {
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

    @GetMapping("/songs/{id}/liked")
    @Operation(
        summary = "Get song liked status",
        description = "Check if a song is liked by the authenticated user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Song liked status retrieved successfully",
            content = @Content(schema = @Schema(implementation = SongLikedResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized (invalid or missing JWT token)"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<SongLikedResponse> getSongLikedStatus(
        @Parameter(description = "Song UUID", required = true, example = "770e8400-e29b-41d4-a716-446655440002")
        @PathVariable UUID id) {
        try {
            String username = getAuthenticatedUsername();
            
            // Check if song is liked
            boolean liked = userService.isSongLiked(username, id);
            
            return ResponseEntity.ok(new SongLikedResponse(liked));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Error getting song liked status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/songs/{id}/liked")
    @Operation(
        summary = "Mark song as liked",
        description = "Mark a song as liked by the authenticated user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Song marked as liked successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized (invalid or missing JWT token)"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<?> markSongAsLiked(
        @Parameter(description = "Song UUID", required = true, example = "770e8400-e29b-41d4-a716-446655440002")
        @PathVariable UUID id) {
        try {
            String username = getAuthenticatedUsername();
            
            // Mark song as liked in UserSong
            userService.markSongAsLiked(username, id);
            
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error marking song as liked: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/songs/{id}/unliked")
    @Operation(
        summary = "Mark song as unliked",
        description = "Mark a song as unliked by the authenticated user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Song marked as unliked successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized (invalid or missing JWT token)"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<?> markSongAsUnliked(
        @Parameter(description = "Song UUID", required = true, example = "770e8400-e29b-41d4-a716-446655440002")
        @PathVariable UUID id) {
        try {
            String username = getAuthenticatedUsername();
            
            // Mark song as unliked in UserSong
            userService.markSongAsUnliked(username, id);
            
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error marking song as unliked: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
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
