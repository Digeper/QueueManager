package org.muzika.queuemanager.kafkaMassages;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestSlskdSong {

    private UUID id;
    private String title;
    private String artist;


}
