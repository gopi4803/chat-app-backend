package com.example.chat_app.websocket;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import com.example.chat_app.model.ChatMessage;
import java.time.Instant;

/**
 * Listens for websocket connect/disconnect events.
 */
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();
        String user = sha.getUser() != null ? sha.getUser().getName() : "ANONYMOUS";
        logger.info("New WebSocket connection. sessionId={}, user={}", sessionId, user);

        // Optionally notify others of join
        ChatMessage joinMsg = new ChatMessage();
        joinMsg.setType(ChatMessage.MessageType.JOIN);
        joinMsg.setFrom(user);
        joinMsg.setContent(user + " joined the chat");
        joinMsg.setTimestamp(Instant.now().toEpochMilli());
        messagingTemplate.convertAndSend("/topic/public", joinMsg);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();
        String user = sha.getUser() != null ? sha.getUser().getName() : "ANONYMOUS";
        logger.info("WebSocket disconnected. sessionId={}, user={}", sessionId, user);

        // Optionally notify others of leave
        ChatMessage leaveMsg = new ChatMessage();
        leaveMsg.setType(ChatMessage.MessageType.LEAVE);
        leaveMsg.setFrom(user);
        leaveMsg.setContent(user + " left the chat");
        leaveMsg.setTimestamp(Instant.now().toEpochMilli());
        messagingTemplate.convertAndSend("/topic/public", leaveMsg);
    }
}
