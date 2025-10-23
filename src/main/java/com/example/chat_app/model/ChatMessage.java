package com.example.chat_app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message payload used for STOMP messages.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private MessageType type;
    private String content;
    private String from;
    private String to;
    private long timestamp;

    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE
    }
}
