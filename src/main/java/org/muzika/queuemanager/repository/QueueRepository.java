package org.muzika.queuemanager.repository;


import org.muzika.queuemanager.entities.Queue;
import org.muzika.queuemanager.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface QueueRepository extends JpaRepository<Queue, User> {


    Queue getReferenceByUser_UserName(String username);
}
