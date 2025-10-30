package com.example.chat_app.controller;

import com.example.chat_app.model.ChatMessage;
import com.example.chat_app.model.ChatMessageEntity;
import com.example.chat_app.service.ChatMessageService;
import com.example.chat_app.service.UserService;
import com.example.chat_app.model.User;
import com.example.chat_app.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ChatHistoryController {

    private final ChatMessageService chatMessageService;
    private final JwtUtils jwtUtils;
    private final HttpServletRequest request;
    private final UserService userService;

    private final Logger logger = LoggerFactory.getLogger(ChatHistoryController.class);

    private String resolveEmail(Authentication auth) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String emailFromToken = jwtUtils.getEmailFromJwtToken(token);
                if (emailFromToken != null && !emailFromToken.isBlank()) {
                    return emailFromToken.toLowerCase();
                }
            } catch (Exception ex) {
                logger.debug("Failed to extract email from JWT: {}", ex.getMessage());
            }
        }

        if (auth != null && auth.getName() != null) {
            return auth.getName().toLowerCase();
        }

        return null;
    }

    @GetMapping("/{otherUserEmail}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @PathVariable String otherUserEmail,
            Authentication auth) {

        String me = resolveEmail(auth);
        if (me == null) return ResponseEntity.status(401).build();

        logger.debug("getChatHistory: me={} other={}", me, otherUserEmail);
        List<ChatMessage> history = chatMessageService.getChatHistory(me, otherUserEmail.toLowerCase());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<Map<String, Object>>> getUserConversations(Authentication auth) {
        String me = resolveEmail(auth);
        if (me == null) return ResponseEntity.status(401).build();

        logger.debug("getUserConversations for {}", me);
        List<ChatMessageEntity> all = chatMessageService.getAllMessagesOfUser(me);

        Map<String, ChatMessageEntity> latestMap = new HashMap<>();
        for (ChatMessageEntity m : all) {
            String other = m.getFromUser().equalsIgnoreCase(me) ? m.getToUser() : m.getFromUser();
            if (other == null) continue;
            ChatMessageEntity existing = latestMap.get(other);
            if (existing == null || m.getTimestamp() > existing.getTimestamp()) {
                latestMap.put(other, m);
            }
        }

        List<Map<String, Object>> result = latestMap.entrySet().stream()
                .map(e -> {
                    String otherEmail = e.getKey();
                    ChatMessageEntity latest = e.getValue();
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", otherEmail);
                    map.put("lastMessage", latest.getContent());
                    map.put("lastAt", latest.getTimestamp());

                    // Try to get the user's username; fallback to email if not found
                    try {
                        Optional<User> userOpt = userService.getUserByEmail(otherEmail);
                        if (userOpt.isPresent()) {
                            map.put("name", userOpt.get().getUsername());
                        } else {
                            map.put("name", otherEmail);
                        }
                    } catch (Exception ex) {
                        map.put("name", otherEmail);
                    }

                    return map;
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/sync")
    public ResponseEntity<List<ChatMessage>> syncMessages(
            @RequestParam("since") long since,
            Authentication auth) {

        String me = resolveEmail(auth);
        if (me == null) return ResponseEntity.status(401).build();

        logger.debug("syncMessages: me={} since={}", me, since);
        List<ChatMessage> newMessages = chatMessageService.getMessagesAfter(me, since);

        logger.debug("Returning {} new messages for {}", newMessages.size(), me);
        return ResponseEntity.ok(newMessages);
    }
}
