package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/mentors")
    public List<User> getMentors() {
        return userRepository.findByRole("MENTOR");
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/conversations/{userId}")
    public List<User> getConversations(@PathVariable Long userId) {
        return userRepository.findConversations(userId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();
        if (userDetails.getBio() != null) user.setBio(userDetails.getBio());
        if (userDetails.getSkills() != null) user.setSkills(userDetails.getSkills());
        if (userDetails.getActiveHours() != null) user.setActiveHours(userDetails.getActiveHours());
        if (userDetails.getEmail() != null) user.setEmail(userDetails.getEmail());
        
        User updatedUser = userRepository.save(user);
        return ResponseEntity.ok(updatedUser);
    }
}
