package com.example.demo.repository;

import com.example.demo.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByDoubtIdOrderByCreatedAtAsc(Long doubtId);

    @Query("SELECT m FROM Message m WHERE m.isPrivate = true AND " +
           "((m.sender.id = :user1Id AND m.recipient.id = :user2Id) OR " +
           " (m.sender.id = :user2Id AND m.recipient.id = :user1Id)) " +
           "ORDER BY m.createdAt ASC")
    List<Message> findPrivateMessages(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}
