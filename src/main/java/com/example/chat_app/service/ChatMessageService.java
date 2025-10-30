package com.example.chat_app.service;

import com.example.chat_app.model.ChatMessage;
import com.example.chat_app.model.ChatMessageEntity;
import com.example.chat_app.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

    private final ChatMessageRepository messageRepo;

    public void saveMessage(ChatMessage chatMessage) {
        chatMessage.setFrom(chatMessage.getFrom().toLowerCase());
        chatMessage.setTo(chatMessage.getTo() != null ? chatMessage.getTo().toLowerCase() : null);

        // Generate a messageId if missing
        if (chatMessage.getMessageId() == null || chatMessage.getMessageId().isBlank()) {
            chatMessage.setMessageId(UUID.randomUUID().toString());
        }

        // Avoid duplicates
        Optional<ChatMessageEntity> existing = messageRepo.findByMessageId(chatMessage.getMessageId());
        if (existing.isPresent()) {
            log.debug("Duplicate messageId {} ignored", chatMessage.getMessageId());
            return;
        }

        ChatMessageEntity entity = ChatMessageEntity.fromChatMessage(chatMessage);
        entity.setDelivered(true);

        messageRepo.save(entity);
        log.debug("Saved message [{}] {} -> {} ({})", chatMessage.getMessageId(), chatMessage.getFrom(), chatMessage.getTo(), chatMessage.getContent());
    }

    public List<ChatMessage> getChatHistory(String user1, String user2) {
        return messageRepo.findChatHistory(user1, user2).stream()
                .map(ChatMessageEntity::toChatMessage)
                .toList();
    }

    public List<ChatMessageEntity> getAllMessagesOfUser(String email) {
        return messageRepo.findAllMessagesOfUser(email);
    }

    public List<ChatMessage> getMessagesAfter(String email, long since) {
        List<ChatMessage> messages = messageRepo.findMessagesAfter(email, since)
                .stream()
                .map(ChatMessageEntity::toChatMessage)
                .toList();
        log.info("getMessagesAfter() called with email={} since={} → {}", email, since, messages.size());
        return messages;
    }

    public void markMessagesAsRead(String reader, String sender) {
        List<ChatMessageEntity> unreadMessages = messageRepo.findUnreadMessages(sender, reader);
        long now = System.currentTimeMillis();

        for (ChatMessageEntity msg : unreadMessages) {
            msg.setReadAt(now);
            msg.setDelivered(true);
        }

        messageRepo.saveAll(unreadMessages);
        log.info(" Marked {} messages as read from {} -> {}", unreadMessages.size(), sender, reader);
    }
}
