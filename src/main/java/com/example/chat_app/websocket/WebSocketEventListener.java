package com.example.chat_app.websocket;

import com.example.chat_app.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String user = sha.getUser() != null ? sha.getUser().getName().toLowerCase() : null;
        if (user == null) return;

        onlineUsers.add(user);
        logger.info("WebSocket connected: {}", user);

        ChatMessage presence = new ChatMessage();
        presence.setType(ChatMessage.MessageType.PRESENCE);
        presence.setFrom("system");
        presence.setTo(user);
        presence.setTimestamp(Instant.now().toEpochMilli());
        presence.setContent(user + " is online");

        // broadcast updated presence map
        messagingTemplate.convertAndSend("/topic/presence",
                new PresencePayload(user, true));
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String user = sha.getUser() != null ? sha.getUser().getName().toLowerCase() : null;
        if (user == null) return;

        onlineUsers.remove(user);
        logger.info("WebSocket disconnected: {}", user);

        messagingTemplate.convertAndSend("/topic/presence",
                new PresencePayload(user, false));
    }

    record PresencePayload(String email, boolean online) {}
}
