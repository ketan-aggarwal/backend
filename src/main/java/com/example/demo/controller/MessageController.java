package com.example.demo.controller;

import com.example.demo.model.Doubt;
import com.example.demo.model.Message;
import com.example.demo.model.User;
import com.example.demo.repository.DoubtRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.ProfanityFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoubtRepository doubtRepository;

    @Autowired
    private ProfanityFilter profanityFilter;

    @GetMapping("/doubt/{doubtId}")
    public List<Message> getDoubtReplies(@PathVariable Long doubtId) {
        return messageRepository.findByDoubtIdOrderByCreatedAtAsc(doubtId);
    }

    @PostMapping("/doubt/{doubtId}")
    public ResponseEntity<?> addDoubtReply(@PathVariable Long doubtId, @RequestBody Message message) {
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Reply content cannot be empty!");
        }
        if (profanityFilter.containsProfanity(message.getContent())) {
            return ResponseEntity.badRequest().body("Inappropriate language detected. Please review your text.");
        }
        Optional<Doubt> doubtOpt = doubtRepository.findById(doubtId);
        if (doubtOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (message.getSender() == null || message.getSender().getId() == null) {
            return ResponseEntity.badRequest().body("Sender with ID is required");
        }
        Optional<User> senderOpt = userRepository.findById(message.getSender().getId());
        if (senderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Sender not found");
        }
        
        message.setDoubt(doubtOpt.get());
        message.setSender(senderOpt.get());
        message.setIsPrivate(false);
        Message saved = messageRepository.save(message);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/private")
    public ResponseEntity<?> getPrivateMessages(@RequestParam Long user1, @RequestParam Long user2) {
        return ResponseEntity.ok(messageRepository.findPrivateMessages(user1, user2));
    }

    @PostMapping("/private")
    public ResponseEntity<?> sendPrivateMessage(@RequestBody Message message) {
        if (message.getSender() == null || message.getSender().getId() == null ||
            message.getRecipient() == null || message.getRecipient().getId() == null) {
            return ResponseEntity.badRequest().body("Sender and Recipient IDs are required");
        }
        Optional<User> senderOpt = userRepository.findById(message.getSender().getId());
        Optional<User> recipientOpt = userRepository.findById(message.getRecipient().getId());
        
        if (senderOpt.isEmpty() || recipientOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Sender or Recipient not found");
        }
        
        message.setSender(senderOpt.get());
        message.setRecipient(recipientOpt.get());
        message.setIsPrivate(true);
        message.setDoubt(null);
        if (message.getContent() != null) {
            message.setContent(profanityFilter.censorText(message.getContent()));
        }
        Message saved = messageRepository.save(message);
        return ResponseEntity.ok(saved);
    }
}
