package com.example.chat_app.controller;

import com.example.chat_app.websocket.WebSocketEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/presence")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class PresenceController {

    private final WebSocketEventListener webSocketEventListener;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getPresenceSnapshot() {
        Set<String> onlineUsers = webSocketEventListener.getOnlineUsers();
        Map<String, Long> lastSeen = webSocketEventListener.getLastSeenMap();

        List<Map<String, Object>> snapshot = new ArrayList<>();

        // online users
        for (String email : onlineUsers) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("email", email);
            entry.put("online", true);
            entry.put("lastSeen", null);
            snapshot.add(entry);
        }

        // offline users with last seen
        for (Map.Entry<String, Long> e : lastSeen.entrySet()) {
            if (!onlineUsers.contains(e.getKey())) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("email", e.getKey());
                entry.put("online", false);
                entry.put("lastSeen", e.getValue());
                snapshot.add(entry);
            }
        }

        return ResponseEntity.ok(snapshot);
    }
}
