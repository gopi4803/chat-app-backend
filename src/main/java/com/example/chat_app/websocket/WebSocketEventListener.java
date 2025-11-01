package com.example.chat_app.websocket;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<String, AtomicInteger> sessions = new ConcurrentHashMap<>();

    public Set<String> getOnlineUsers() {
        return sessions.keySet();
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String email = sha.getUser() != null ? sha.getUser().getName().toLowerCase() : null;
        if (email == null) return;

        sessions.compute(email, (k, v) -> v == null ? new AtomicInteger(1) : new AtomicInteger(v.incrementAndGet()));
        log.info(" {} connected ({} sessions)", email, sessions.get(email).get());

        //  broadcast "online"
        messagingTemplate.convertAndSend("/topic/presence", new PresencePayload(email, true));

        //  after a small delay, send full presence snapshot only to this user
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    List<PresencePayload> snapshot = sessions.keySet().stream()
                            .map(u -> new PresencePayload(u, true))
                            .collect(Collectors.toList());
                    messagingTemplate.convertAndSendToUser(email, "/queue/presence", snapshot);
                    log.info(" Sent presence snapshot to {}", email);
                } catch (Exception ex) {
                    log.warn("Failed to send snapshot to {}: {}", email, ex.getMessage());
                }
            }
        }, 400); // delay ensures Spring finished mapping the session
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String email = sha.getUser() != null ? sha.getUser().getName().toLowerCase() : null;
        if (email == null) return;

        sessions.computeIfPresent(email, (k, v) -> {
            if (v.decrementAndGet() <= 0) {
                sessions.remove(k);
                log.info("{} went offline", email);
                messagingTemplate.convertAndSend("/topic/presence", new PresencePayload(email, false));
                return null;
            }
            return v;
        });
    }

    public record PresencePayload(String email, boolean online) {}
}
