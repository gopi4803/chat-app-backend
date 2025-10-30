package com.example.chat_app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_message_id", columnList = "messageId", unique = true),
        @Index(name = "idx_from_to_ts", columnList = "fromUser,toUser,timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String messageId;

    private String fromUser;
    private String toUser;

    @Column(columnDefinition = "TEXT")
    private String content;

    private long timestamp;

    @Enumerated(EnumType.STRING)
    private ChatMessage.MessageType type;

    private boolean delivered = false;

    private Long readAt;

    public static ChatMessageEntity fromChatMessage(ChatMessage msg) {
        return ChatMessageEntity.builder()
                .messageId(msg.getMessageId())
                .fromUser(msg.getFrom())
                .toUser(msg.getTo())
                .content(msg.getContent())
                .timestamp(msg.getTimestamp())
                .type(msg.getType())
                .delivered(msg.isDelivered())
                .readAt(msg.getReadAt())
                .build();
    }

    public ChatMessage toChatMessage() {
        ChatMessage m = new ChatMessage();
        m.setMessageId(this.messageId);
        m.setType(this.type);
        m.setContent(this.content);
        m.setFrom(this.fromUser);
        m.setTo(this.toUser);
        m.setTimestamp(this.timestamp);
        m.setDelivered(this.delivered);
        m.setReadAt(this.readAt);
        return m;
    }
}
