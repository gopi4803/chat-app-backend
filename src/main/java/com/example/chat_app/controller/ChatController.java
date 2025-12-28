package com.example.chat_app.controller;

import com.example.chat_app.model.ChatMessage;
import com.example.chat_app.model.ChatMessageEntity;
import com.example.chat_app.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Map;
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

        if (authentication != null)
            chatMessage.setFrom(authentication.getName().toLowerCase());

        chatMessage.setTo(chatMessage.getTo().toLowerCase());

        if (chatMessage.getMessageId() == null)
            chatMessage.setMessageId(UUID.randomUUID().toString());

        // DO NOT mark delivered yet
        chatMessage.setDelivered(false);

        chatMessageService.saveMessage(chatMessage);

        // Send to receiver
        messagingTemplate.convertAndSendToUser(
                chatMessage.getTo(), "/queue/messages", chatMessage
        );

        // Send echo to sender
        messagingTemplate.convertAndSendToUser(
                chatMessage.getFrom(), "/queue/messages", chatMessage
        );
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

    @MessageMapping("/chat.delivered")
    public void markDelivered(@Payload Map<String, String> payload, Authentication auth) {
        if (auth == null) return;

        String messageId = payload.get("messageId");
        if (messageId == null) return;

        chatMessageService.markDeliveredByMessageId(messageId);

        // Notify sender so ✓✓ updates immediately
        ChatMessageEntity ent = chatMessageService.getByMessageId(messageId);
        if (ent != null) {
            messagingTemplate.convertAndSendToUser(
                    ent.getFromUser(),
                    "/queue/messages",
                    ent.toChatMessage()
            );
        }
    }

}
