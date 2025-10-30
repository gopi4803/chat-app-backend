package com.example.chat_app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fromUser;
    private String toUser;

    @Column(columnDefinition = "TEXT")
    private String content;

    private long timestamp;

    @Enumerated(EnumType.STRING)
    private ChatMessage.MessageType type;

    private boolean delivered = false;

    public static ChatMessageEntity fromChatMessage(ChatMessage msg) {
        return ChatMessageEntity.builder()
                .fromUser(msg.getFrom())
                .toUser(msg.getTo())
                .content(msg.getContent())
                .timestamp(msg.getTimestamp())
                .type(msg.getType())
                .delivered(false)
                .build();
    }

    public ChatMessage toChatMessage() {
        ChatMessage m = new ChatMessage();
        m.setType(this.type);
        m.setContent(this.content);
        m.setFrom(this.fromUser);
        m.setTo(this.toUser);
        m.setTimestamp(this.timestamp);
        return m;
    }
}
