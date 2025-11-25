package org.muzika.queuemanager.kafkaMassages;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoadedSong {


    private UUID uuid;

    private String filePath;
    private Status status;




    public enum Status {
        COMPLETED,
        ERROR
    }

}
