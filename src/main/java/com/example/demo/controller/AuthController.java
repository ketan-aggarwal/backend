package com.example.demo.controller;

import com.example.demo.dto.GoogleAuthRequest;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Value("${google.client.id}")
    private String googleClientId;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username is already taken!");
        }
        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
        if (userOpt.isEmpty() || !userOpt.get().getPassword().equals(loginRequest.getPassword())) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }
        return ResponseEntity.ok(userOpt.get());
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleAuthRequest request) {
        try {
            if (googleClientId == null || googleClientId.isEmpty()) {
                return ResponseEntity.badRequest().body("Google Client ID is not configured on the server!");
            }

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                return ResponseEntity.status(401).body("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            
            if (email == null) {
                return ResponseEntity.badRequest().body("Email not provided by Google OAuth");
            }

            List<User> users = userRepository.findByEmail(email);
            User user = null;
            if (!users.isEmpty()) {
                // If there are multiple accounts, try to match the requested role context
                String targetRole = request.getRole() != null ? request.getRole() : "STUDENT";
                for (User u : users) {
                    if (u.getRole().equalsIgnoreCase(targetRole)) {
                        user = u;
                        break;
                    }
                }
                // Fallback to the first found user if no role matched
                if (user == null) {
                    user = users.get(0);
                }
            } else {
                // Register a new user
                user = new User();
                user.setEmail(email);
                user.setRole(request.getRole() != null ? request.getRole() : "STUDENT");
                
                // Construct a unique username from the name
                String baseUsername = (name != null && !name.trim().isEmpty()) 
                        ? name.replaceAll("\\s+", "").toLowerCase() 
                        : email.split("@")[0];
                
                String uniqueUsername = baseUsername;
                int count = 1;
                while (userRepository.findByUsername(uniqueUsername).isPresent()) {
                    uniqueUsername = baseUsername + count;
                    count++;
                }
                user.setUsername(uniqueUsername);
                
                // Random secure password for OAuth user
                user.setPassword(UUID.randomUUID().toString());
                user = userRepository.save(user);
            }

            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Google Auth error: " + e.getMessage());
        }
    }
}
