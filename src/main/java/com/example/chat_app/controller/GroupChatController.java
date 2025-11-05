package com.example.chat_app.controller;

import com.example.chat_app.model.ChatMessage;
import com.example.chat_app.model.User;
import com.example.chat_app.service.GroupMessageService;
import com.example.chat_app.service.GroupService;
import com.example.chat_app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class GroupChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GroupMessageService groupMessageService;
    private final GroupService groupService;
    private final UserService userService;

    /**
     * Client sends to /app/group.send with payload containing:
     * { groupId: <number>, content: "...", messageId?: "...", type: "CHAT" }
     * Server saves and broadcasts to /topic/group.{groupId}
     */
    @MessageMapping("/group.send")
    public void sendGroupMessage(@Payload GroupWsPayload p, Authentication auth) {
        if (auth == null) return;
        String sender = auth.getName().toLowerCase();
        long ts = Instant.now().toEpochMilli();

        String messageId = p.getMessageId();
        if (messageId == null || messageId.isBlank()) messageId = UUID.randomUUID().toString();

        // persist message first
        var ent = groupMessageService.saveGroupMessage(
                p.getGroupId(),
                sender,
                p.getContent(),
                ChatMessage.MessageType.CHAT,
                messageId,
                ts
        );

        String displayName = userService.getUserByEmail(sender)
                .map(User::getUsername)
                .orElse(sender);

        // Build broadcast payload
        GroupWsBroadcast out = new GroupWsBroadcast();
        out.setMessageId(messageId);
        out.setGroupId(p.getGroupId());
        out.setSender(sender);
        out.setSenderName(displayName);
        out.setContent(p.getContent());
        out.setTimestamp(ent.getTimestamp());
        out.setDelivered(ent.isDelivered());
        out.setType(ChatMessage.MessageType.CHAT);

        // Broadcast to all subscribers of /topic/group.{id}
        messagingTemplate.convertAndSend("/topic/group." + p.getGroupId(), out);
    }


    @MessageMapping("/group.typing")
    public void handleTyping(@Payload GroupTypingPayload p, Authentication auth) {
        if (auth == null) return;
        String who = auth.getName().toLowerCase();
        GroupTypingBroadcast b = new GroupTypingBroadcast();
        b.setGroupId(p.getGroupId());
        b.setFrom(who);
        b.setTimestamp(System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/group." + p.getGroupId() + ".typing", b);
    }

    // DTOs
    public static class GroupWsPayload {
        private Long groupId;
        private String content;
        private String messageId;
        private ChatMessage.MessageType type;
        // getters/setters
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public ChatMessage.MessageType getType() { return type; }
        public void setType(ChatMessage.MessageType type) { this.type = type; }
    }

    public static class GroupWsBroadcast {
        private String messageId;
        private Long groupId;
        private String sender;
        private String senderName;
        private String content;
        private long timestamp;
        private boolean delivered;
        private ChatMessage.MessageType type;

        // getters / setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }

        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }

        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }

        public String getSenderName() { return senderName; }
        public void setSenderName(String senderName) { this.senderName = senderName; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public boolean isDelivered() { return delivered; }
        public void setDelivered(boolean delivered) { this.delivered = delivered; }

        public ChatMessage.MessageType getType() { return type; }
        public void setType(ChatMessage.MessageType type) { this.type = type; }
    }

    public static class GroupTypingPayload {
        private Long groupId;
        // getters/setters
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
    }

    public static class GroupTypingBroadcast {
        private Long groupId;
        private String from;
        private long timestamp;
        // getters/setters
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
