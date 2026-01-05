package org.muzika.queuemanager.services;

import org.muzika.queuemanager.dto.AddSongToQueueRequest;
import org.muzika.queuemanager.dto.QueueResponse;
import org.muzika.queuemanager.dto.SongDTO;
import org.muzika.queuemanager.entities.Queue;
import org.muzika.queuemanager.entities.Song;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class QueueController {

    private final QueueService queueService;
    private final QueueCheckerService queueCheckerService;

    public QueueController(QueueService queueService, QueueCheckerService queueCheckerService) {
        this.queueService = queueService;
        this.queueCheckerService = queueCheckerService;
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
        try {
            String username = getAuthenticatedUsername();
            Queue queue = queueService.getQueueByUsername(username);
            
            if (queue == null || queue.getSongs() == null || queue.getSongs().isEmpty()) {
                QueueResponse response = new QueueResponse();
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
