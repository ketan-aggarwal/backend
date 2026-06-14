package com.example.demo.repository;

import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByEmail(String email);
    List<User> findByRole(String role);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM User u WHERE u.id <> :userId AND " +
           "(u.id IN (SELECT m.sender.id FROM Message m WHERE m.isPrivate = true AND m.recipient.id = :userId) OR " +
           " u.id IN (SELECT m.recipient.id FROM Message m WHERE m.isPrivate = true AND m.sender.id = :userId))")
    List<User> findConversations(@org.springframework.data.repository.query.Param("userId") Long userId);
}
