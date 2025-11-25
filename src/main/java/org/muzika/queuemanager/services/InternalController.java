package org.muzika.queuemanager.services;


import org.apache.kafka.clients.producer.KafkaProducer;
import org.muzika.queuemanager.kafkaMassages.RequestRandomSong;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class InternalController {


    private final KafkaProducerService kafka;
    private final QueueManagerService queueManagerService;

    public InternalController(KafkaProducerService kafka, QueueManagerService queueManagerService) {
        this.kafka = kafka;
        this.queueManagerService = queueManagerService;
    }


    @GetMapping("/hello")
    @ResponseBody
    public void RandomSong(){


        UUID uuid = queueManagerService.newSong();

        kafka.send(
                "request-random-song",
                UUID.randomUUID(),
                new RequestRandomSong(uuid,"hisa"));


    }



}