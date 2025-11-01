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

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);
    private final SimpMessagingTemplate messagingTemplate;

    // email- session count
    private final Map<String, AtomicInteger> sessions = new ConcurrentHashMap<>();

    // email - last seen timestamp
    private final Map<String, Long> lastSeenMap = new ConcurrentHashMap<>();

    public Set<String> getOnlineUsers() {
        return sessions.keySet();
    }

    public Map<String, Long> getLastSeenMap() {
        return lastSeenMap;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String email = sha.getUser() != null ? sha.getUser().getName().toLowerCase() : null;
        if (email == null) return;

        sessions.compute(email, (k, v) -> v == null ? new AtomicInteger(1) : new AtomicInteger(v.incrementAndGet()));
        lastSeenMap.remove(email);

        log.info(" {} connected ({} sessions)", email, sessions.get(email).get());

        // broadcast "online"
        messagingTemplate.convertAndSend("/topic/presence",
                new PresencePayload(email, true, null));

        // send full presence snapshot to this user after delay
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                List<PresencePayload> snapshot = new ArrayList<>();

                // online users
                sessions.keySet().forEach(u ->
                        snapshot.add(new PresencePayload(u, true, null))
                );

                // offline users (those with last seen timestamps)
                lastSeenMap.forEach((user, last) -> {
                    if (!sessions.containsKey(user)) {
                        snapshot.add(new PresencePayload(user, false, last));
                    }
                });

                try {
                    messagingTemplate.convertAndSendToUser(email, "/queue/presence", snapshot);
                    log.info(" Sent presence snapshot to {}", email);
                } catch (Exception ex) {
                    log.warn("️ Failed to send presence snapshot: {}", ex.getMessage());
                }
            }
        }, 400);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String email = sha.getUser() != null ? sha.getUser().getName().toLowerCase() : null;
        if (email == null) return;

        sessions.computeIfPresent(email, (k, v) -> {
            if (v.decrementAndGet() <= 0) {
                sessions.remove(k);
                long now = System.currentTimeMillis();
                lastSeenMap.put(k, now);

                messagingTemplate.convertAndSend("/topic/presence",
                        new PresencePayload(k, false, now));
                log.info(" {} went offline at {}", k, new Date(now));
                return null;
            }
            return v;
        });
    }

    public record PresencePayload(String email, boolean online, Long lastSeen) {}
}
