package com.example.chat_app.controller;

import com.example.chat_app.model.ChatMessage;
import com.example.chat_app.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.Instant;

/**
 * Handles chat messages from clients.
 * WebSocket path: /app/chat.send
 */
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage, Authentication authentication) {
        chatMessage.setTimestamp(Instant.now().toEpochMilli());

        //  Normalize identities
        if (authentication != null && authentication.getName() != null) {
            chatMessage.setFrom(authentication.getName().toLowerCase());
        }

        if (chatMessage.getTo() != null) {
            chatMessage.setTo(chatMessage.getTo().toLowerCase());
        }

        //  Save only once — no REST duplicate
        chatMessageService.saveMessage(chatMessage);

        //  Private message routing
        if (chatMessage.getTo() != null && !chatMessage.getTo().isBlank()) {
            String sender = chatMessage.getFrom();
            String receiver = chatMessage.getTo();

            messagingTemplate.convertAndSendToUser(receiver, "/queue/messages", chatMessage);
            messagingTemplate.convertAndSendToUser(sender, "/queue/messages", chatMessage);
            System.out.printf("📨 Private message %s -> %s : %s%n", sender, receiver, chatMessage.getContent());
        } else {
            // Public fallback
            messagingTemplate.convertAndSend("/topic/public", chatMessage);
            System.out.printf("🌍 Public broadcast from %s : %s%n", chatMessage.getFrom(), chatMessage.getContent());
        }
    }

    @MessageMapping("/chat.join")
    public void join(@Payload ChatMessage chatMessage, Authentication authentication) {
        chatMessage.setTimestamp(Instant.now().toEpochMilli());
        if (authentication != null && authentication.getName() != null) {
            chatMessage.setFrom(authentication.getName().toLowerCase());
        }
        chatMessage.setType(ChatMessage.MessageType.JOIN);
        chatMessage.setContent(chatMessage.getFrom() + " joined the chat");
        messagingTemplate.convertAndSend("/topic/public", chatMessage);
    }
}
