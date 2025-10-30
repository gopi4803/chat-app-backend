package com.example.chat_app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String messageId;
    private MessageType type;
    private String content;
    private String from;
    private String to;
    private long timestamp;
    private boolean delivered;
    private Long readAt;

    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE,
        READ_RECEIPT,
        PRESENCE,
        TYPING
    }
}
