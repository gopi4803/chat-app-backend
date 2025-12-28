package com.example.chat_app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "group_messages",
        indexes = @Index(name = "idx_group_ts", columnList = "group_id, timestamp")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    private String sender;

    @Column(name = "display_name")
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String content;

    private long timestamp;

    @Enumerated(EnumType.STRING)
    private ChatMessage.MessageType type;

    /**
     Deprecated should be removed later
     */
    @Deprecated
    private boolean delivered;

    /**
     Deprecated should be removed later
     */
    @Deprecated
    private Long readAt;
}
