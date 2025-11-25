package org.muzika.queuemanager.repository;

import jakarta.transaction.Transactional;
import org.muzika.queuemanager.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Transactional
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUserName(String username);

    User findByUserName(String username);
}
