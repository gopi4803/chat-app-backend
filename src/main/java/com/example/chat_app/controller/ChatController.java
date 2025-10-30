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
import java.util.UUID;

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

        if (authentication != null && authentication.getName() != null)
            chatMessage.setFrom(authentication.getName().toLowerCase());

        if (chatMessage.getTo() != null)
            chatMessage.setTo(chatMessage.getTo().toLowerCase());

        // Ensure messageId exists
        if (chatMessage.getMessageId() == null || chatMessage.getMessageId().isBlank()) {
            chatMessage.setMessageId(UUID.randomUUID().toString());
        }

        chatMessage.setDelivered(true);
        chatMessageService.saveMessage(chatMessage);

        // Echo to both sender & receiver
        messagingTemplate.convertAndSendToUser(chatMessage.getTo(), "/queue/messages", chatMessage);
        messagingTemplate.convertAndSendToUser(chatMessage.getFrom(), "/queue/messages", chatMessage);

        System.out.printf(" %s [%s] %s -> %s : %s%n",
                chatMessage.getMessageId(),
                chatMessage.getType(),
                chatMessage.getFrom(),
                chatMessage.getTo(),
                chatMessage.getContent());
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

    @MessageMapping("/chat.read")
    public void markAsRead(@Payload ChatMessage readNotification, Authentication authentication) {
        if (authentication == null) return;
        String reader = authentication.getName().toLowerCase();

        chatMessageService.markMessagesAsRead(reader, readNotification.getFrom());

        ChatMessage receipt = new ChatMessage();
        receipt.setMessageId(UUID.randomUUID().toString());
        receipt.setType(ChatMessage.MessageType.READ_RECEIPT);
        receipt.setFrom(reader);
        receipt.setTo(readNotification.getFrom());
        receipt.setTimestamp(Instant.now().toEpochMilli());
        receipt.setDelivered(true);
        receipt.setReadAt(receipt.getTimestamp());

        // Send read receipt to sender
        messagingTemplate.convertAndSendToUser(readNotification.getFrom(), "/queue/messages", receipt);
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload ChatMessage typingMsg, Authentication auth) {
        if (auth == null) return;
        String from = auth.getName().toLowerCase();
        typingMsg.setFrom(from);
        typingMsg.setType(ChatMessage.MessageType.TYPING);
        typingMsg.setTimestamp(System.currentTimeMillis());

        // Forward typing event to receiver only
        if (typingMsg.getTo() != null) {
            String receiver = typingMsg.getTo().toLowerCase();
            messagingTemplate.convertAndSendToUser(receiver, "/queue/typing", typingMsg);
        }
    }

}
