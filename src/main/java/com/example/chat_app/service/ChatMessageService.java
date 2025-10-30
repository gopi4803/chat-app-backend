package com.example.chat_app.service;

import com.example.chat_app.model.ChatMessage;
import com.example.chat_app.model.ChatMessageEntity;
import com.example.chat_app.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

    private final ChatMessageRepository messageRepo;

    public void saveMessage(ChatMessage chatMessage) {
        //  Always normalize case before persisting
        chatMessage.setFrom(chatMessage.getFrom().toLowerCase());
        chatMessage.setTo(chatMessage.getTo() != null ? chatMessage.getTo().toLowerCase() : null);
        messageRepo.save(ChatMessageEntity.fromChatMessage(chatMessage));
        log.debug("Saved message: {} -> {} [{}]", chatMessage.getFrom(), chatMessage.getTo(), chatMessage.getContent());
    }

    public List<ChatMessage> getChatHistory(String user1, String user2) {
        List<ChatMessage> history = messageRepo.findChatHistory(user1, user2)
                .stream()
                .map(ChatMessageEntity::toChatMessage)
                .toList();
        log.debug("Fetched {} messages between {} and {}", history.size(), user1, user2);
        return history;
    }

    public List<ChatMessageEntity> getAllMessagesOfUser(String email) {
        List<ChatMessageEntity> all = messageRepo.findAllMessagesOfUser(email);
        log.debug("Fetched {} total messages for {}", all.size(), email);
        return all;
    }

    public List<ChatMessage> getMessagesAfter(String email, long since) {
        List<ChatMessage> messages = messageRepo.findMessagesAfter(email, since)
                .stream()
                .map(ChatMessageEntity::toChatMessage)
                .toList();
        log.debug("Fetched {} messages for {} since {}", messages.size(), email, since);
        log.info(" getMessagesAfter() called with email={} since={}", email, since);
        return messages;
    }
}
