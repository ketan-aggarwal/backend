package com.example.demo.handler;

import com.example.demo.model.Message;
import com.example.demo.model.User;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    public ChatWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // Enables Java 8 Date/Time serialization (LocalDateTime)
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) return;

        String query = uri.getQuery();
        Long userId = null;
        if (query != null && query.contains("userId=")) {
            try {
                String userIdStr = query.split("userId=")[1].split("&")[0];
                userId = Long.parseLong(userIdStr);
            } catch (Exception e) {
                System.err.println("Failed to parse userId from query params: " + e.getMessage());
            }
        }

        if (userId != null) {
            userSessions.put(userId, session);
            session.getAttributes().put("userId", userId);
            System.out.println("WebSocket connection established for User ID: " + userId + " (Session: " + session.getId() + ")");
        } else {
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        
        // Parse incoming message JSON
        Map<?, ?> map;
        try {
            map = objectMapper.readValue(payload, Map.class);
        } catch (IOException e) {
            System.err.println("Failed to parse incoming WebSocket message JSON: " + e.getMessage());
            return;
        }

        String content = (String) map.get("content");
        Number senderIdNum = (Number) map.get("senderId");
        Number recipientIdNum = (Number) map.get("recipientId");

        if (content == null || senderIdNum == null || recipientIdNum == null) {
            System.err.println("Invalid payload: content, senderId, and recipientId are required");
            return;
        }

        Long senderId = senderIdNum.longValue();
        Long recipientId = recipientIdNum.longValue();

        Optional<User> senderOpt = userRepository.findById(senderId);
        Optional<User> recipientOpt = userRepository.findById(recipientId);

        if (senderOpt.isEmpty() || recipientOpt.isEmpty()) {
            System.err.println("Sender or Recipient user not found in database");
            return;
        }

        // Create and save the message entity to the database
        Message chatMessage = new Message();
        chatMessage.setContent(content);
        chatMessage.setSender(senderOpt.get());
        chatMessage.setRecipient(recipientOpt.get());
        chatMessage.setIsPrivate(true);
        chatMessage.setCreatedAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(chatMessage);
        String savedMessageJson = objectMapper.writeValueAsString(savedMessage);

        // Deliver message to Recipient if online
        WebSocketSession recipientSession = userSessions.get(recipientId);
        if (recipientSession != null && recipientSession.isOpen()) {
            try {
                recipientSession.sendMessage(new TextMessage(savedMessageJson));
            } catch (IOException e) {
                System.err.println("Failed to send message to recipient: " + e.getMessage());
            }
        }

        // Echo message back to Sender for UI confirmation
        WebSocketSession senderSession = userSessions.get(senderId);
        if (senderSession != null && senderSession.isOpen()) {
            try {
                senderSession.sendMessage(new TextMessage(savedMessageJson));
            } catch (IOException e) {
                System.err.println("Failed to echo message back to sender: " + e.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.remove(userId);
            System.out.println("WebSocket connection closed for User ID: " + userId + " (Session: " + session.getId() + ")");
        }
    }
}
