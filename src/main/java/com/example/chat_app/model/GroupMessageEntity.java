package com.example.chat_app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "group_messages", indexes = {
        @Index(name = "idx_group_ts", columnList = "group_id, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    private String sender;
    @Column(name = "display_name")
    private String displayName;


    @Column(columnDefinition = "TEXT")
    private String content;

    private long timestamp;

    @Enumerated(EnumType.STRING)
    private com.example.chat_app.model.ChatMessage.MessageType type;

    private boolean delivered = false;

    private Long readAt;

    public static GroupMessageEntity fromPayload(String messageId, Group group, String sender, String content, long ts, com.example.chat_app.model.ChatMessage.MessageType type) {
        return GroupMessageEntity.builder()
                .messageId(messageId)
                .group(group)
                .sender(sender)
                .content(content)
                .timestamp(ts)
                .type(type)
                .delivered(true)
                .build();
    }
}
