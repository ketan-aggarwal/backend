package com.example.demo.controller;

import com.example.demo.model.Doubt;
import com.example.demo.model.User;
import com.example.demo.repository.DoubtRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/doubts")
public class DoubtController {

    @Autowired
    private DoubtRepository doubtRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public List<Doubt> getDoubts(@RequestParam(required = false) String category,
                                 @RequestParam(required = false) String status) {
        if (category != null && status != null) {
            return doubtRepository.findByCategoryAndStatus(category, status);
        } else if (category != null) {
            return doubtRepository.findByCategory(category);
        } else if (status != null) {
            return doubtRepository.findByStatus(status);
        } else {
            return doubtRepository.findAll();
        }
    }

    @PostMapping
    public ResponseEntity<?> createDoubt(@RequestBody Doubt doubt) {
        if (doubt.getTitle() == null || doubt.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Doubt title cannot be empty!");
        }
        if (doubt.getTitle().trim().length() < 5 || doubt.getTitle().trim().length() > 100) {
            return ResponseEntity.badRequest().body("Title must be between 5 and 100 characters!");
        }
        if (doubt.getDescription() == null || doubt.getDescription().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Doubt description cannot be empty!");
        }
        if (doubt.getCategory() == null || doubt.getCategory().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Category is required!");
        }
        if (doubt.getStudent() == null || doubt.getStudent().getId() == null) {
            return ResponseEntity.badRequest().body("Student reference with ID is required");
        }
        Optional<User> studentOpt = userRepository.findById(doubt.getStudent().getId());
        if (studentOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Student not found");
        }
        doubt.setStudent(studentOpt.get());
        doubt.setStatus("OPEN");
        Doubt savedDoubt = doubtRepository.save(doubt);
        return ResponseEntity.ok(savedDoubt);
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolveDoubt(@PathVariable Long id, @RequestParam Long mentorId) {
        Optional<Doubt> doubtOpt = doubtRepository.findById(id);
        if (doubtOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Optional<User> mentorOpt = userRepository.findById(mentorId);
        if (mentorOpt.isEmpty() || !mentorOpt.get().getRole().equals("MENTOR")) {
            return ResponseEntity.badRequest().body("Valid mentor ID is required");
        }
        Doubt doubt = doubtOpt.get();
        doubt.setStatus("RESOLVED");
        doubt.setMentor(mentorOpt.get());
        Doubt updatedDoubt = doubtRepository.save(doubt);
        return ResponseEntity.ok(updatedDoubt);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDoubtById(@PathVariable Long id) {
        return doubtRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
