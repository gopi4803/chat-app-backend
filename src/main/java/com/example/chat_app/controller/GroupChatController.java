package com.example.chat_app.controller;

import com.example.chat_app.model.*;
import com.example.chat_app.service.GroupMessageService;
import com.example.chat_app.service.GroupService;
import com.example.chat_app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class GroupChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GroupMessageService groupMessageService;
    private final GroupService groupService;
    private final UserService userService;

    /**
     * Client sends to /app/group.send
     * Payload:
     * {
     *   groupId,
     *   content,
     *   messageId,
     *   type
     * }
     */
    @MessageMapping("/group.send")
    public void sendGroupMessage(@Payload GroupWsPayload p, Authentication auth) {
        if (auth == null) return;
        String sender = auth.getName().toLowerCase();
        long ts = System.currentTimeMillis();

        GroupMessageEntity msg = groupMessageService.saveGroupMessage(
                p.getGroupId(),
                sender,
                p.getContent(),
                ChatMessage.MessageType.CHAT,
                p.getMessageId(),
                ts
        );

        Map<String, Object> out = Map.of(
                "messageId", msg.getMessageId(),
                "groupId", p.getGroupId(),
                "sender", sender,
                "senderName", msg.getDisplayName(),
                "content", msg.getContent(),
                "timestamp", msg.getTimestamp(),
                "type", msg.getType()
        );

        // topic - active viewers
        messagingTemplate.convertAndSend(
                "/topic/group." + p.getGroupId(),
                out
        );

        // queue - guaranteed delivery
        Group group = groupService.getGroup(p.getGroupId()).orElseThrow();
        for (GroupMember member : group.getMembers()) {
            messagingTemplate.convertAndSendToUser(
                    member.getMemberEmail(),
                    "/queue/group.messages",
                    out
            );
        }
    }

    /* GROUP TYPING */
    @MessageMapping("/group.typing")
    public void handleTyping(@Payload GroupTypingPayload p, Authentication auth) {
        if (auth == null) return;

        String who = auth.getName().toLowerCase();

        messagingTemplate.convertAndSend(
                "/topic/group." + p.getGroupId() + ".typing",
                Map.of(
                        "groupId", p.getGroupId(),
                        "from", who,
                        "timestamp", System.currentTimeMillis()
                )
        );
    }

    /* GROUP DELIVERY ACK */
    @MessageMapping("/group.delivered")
    public void markGroupDelivered(@Payload Map<String, Object> payload, Authentication auth) {
        if (auth == null) return;

        Long groupId = Long.valueOf(payload.get("groupId").toString());
        String messageId = payload.get("messageId").toString();
        String user = auth.getName().toLowerCase();

        List<String> deliveredUsers =
                groupMessageService.markDelivered(messageId, user);

        messagingTemplate.convertAndSend(
                "/topic/group." + groupId + ".delivery",
                Map.of(
                        "groupId", groupId,
                        "messageId", messageId,
                        "deliveredRecipients", deliveredUsers
                )
        );
    }

    /* GROUP READ ACK */
    @MessageMapping("/group.read")
    public void markGroupRead(@Payload Map<String, Object> payload, Authentication auth) {
        if (auth == null) return;

        Long groupId = Long.valueOf(payload.get("groupId").toString());
        List<String> messageIds = (List<String>) payload.get("messageIds");
        String reader = auth.getName().toLowerCase();

        for (String mid : messageIds) {
            var reads = groupMessageService.markRead(mid, reader);

            messagingTemplate.convertAndSend(
                    "/topic/group." + groupId + ".read",
                    Map.of(
                            "groupId", groupId,
                            "messageId", mid,
                            "readRecipients",
                            reads.stream()
                                    .map(r -> Map.of(
                                            "email", r.getUserEmail(),
                                            "readAt", r.getReadAt()
                                    ))
                                    .toList()
                    )
            );
        }
    }

    /* DTOs */
    public static class GroupWsPayload {
        private Long groupId;
        private String content;
        private String messageId;
        private ChatMessage.MessageType type;
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public ChatMessage.MessageType getType() { return type; }
        public void setType(ChatMessage.MessageType type) { this.type = type; }
    }

    public static class GroupTypingPayload {
        private Long groupId;
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
    }
}
