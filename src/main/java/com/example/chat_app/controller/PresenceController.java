package com.example.chat_app.controller;

import com.example.chat_app.websocket.WebSocketEventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller to return current online users to rely on user-destination snapshot delivery.
 */
@RestController
public class PresenceController {

    private final WebSocketEventListener webSocketEventListener;

    public PresenceController(WebSocketEventListener webSocketEventListener) {
        this.webSocketEventListener = webSocketEventListener;
    }

    @GetMapping("/presence")
    public ResponseEntity<List<String>> getOnlineUsers() {
        List<String> online = new ArrayList<>(webSocketEventListener.getOnlineUsers());
        return ResponseEntity.ok(online);
    }

}
