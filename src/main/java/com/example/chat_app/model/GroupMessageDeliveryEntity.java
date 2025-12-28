package com.example.chat_app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "group_message_deliveries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_email"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class GroupMessageDeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Long deliveredAt;
}
