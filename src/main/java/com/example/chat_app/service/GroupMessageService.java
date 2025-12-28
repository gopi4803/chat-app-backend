package com.example.chat_app.service;

import com.example.chat_app.model.*;
import com.example.chat_app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GroupMessageService {

    private final GroupMessageRepository messageRepo;
    private final GroupMessageDeliveryRepository deliveryRepo;
    private final GroupMessageReadRepository readRepo;
    private final GroupRepository groupRepository;
    private final UserService userService;

    /* SAVE GROUP MESSAGE */
    public GroupMessageEntity saveGroupMessage(
            Long groupId,
            String sender,
            String content,
            ChatMessage.MessageType type,
            String messageId,
            long timestamp
    ) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        String mid = (messageId == null || messageId.isBlank())
                ? UUID.randomUUID().toString()
                : messageId;

        String displayName = userService.getUserByEmail(sender)
                .map(User::getUsername)
                .orElse(sender);

        GroupMessageEntity msg = GroupMessageEntity.builder()
                .messageId(mid)
                .group(group)
                .sender(sender.toLowerCase())
                .displayName(displayName)
                .content(content)
                .timestamp(timestamp)
                .type(type)
                .build();

        return messageRepo.save(msg);
    }

    /* SYSTEM MESSAGE (LEGACY) */
    public void saveSystemMessage(Long groupId, String content) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        GroupMessageEntity msg = GroupMessageEntity.builder()
                .messageId(UUID.randomUUID().toString())
                .group(group)
                .sender("system")
                .displayName("System")
                .content(content)
                .timestamp(System.currentTimeMillis())
                .type(ChatMessage.MessageType.SYSTEM)
                .build();

        messageRepo.save(msg);
    }

    /* LOAD GROUP HISTORY */
    public List<GroupMessageEntity> getGroupMessages(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        return messageRepo.findByGroupOrderByTimestampAsc(group);
    }

    /* DELIVERY (PER USER) */
    @Transactional
    public List<String> markDelivered(String messageId, String userEmail) {

        if (!deliveryRepo.existsByMessageIdAndUserEmail(messageId, userEmail)) {
            deliveryRepo.save(
                    GroupMessageDeliveryEntity.builder()
                            .messageId(messageId)
                            .userEmail(userEmail)
                            .deliveredAt(System.currentTimeMillis())
                            .build()
            );
        }

        return deliveryRepo.findByMessageId(messageId)
                .stream()
                .map(GroupMessageDeliveryEntity::getUserEmail)
                .distinct()
                .toList();
    }

    /* READ (IMPLIES DELIVERY) */
    @Transactional
    public List<GroupMessageReadEntity> markRead(String messageId, String userEmail) {

        // Ensure delivery exists first
        if (!deliveryRepo.existsByMessageIdAndUserEmail(messageId, userEmail)) {
            deliveryRepo.save(
                    GroupMessageDeliveryEntity.builder()
                            .messageId(messageId)
                            .userEmail(userEmail)
                            .deliveredAt(System.currentTimeMillis())
                            .build()
            );
        }

        if (!readRepo.existsByMessageIdAndUserEmail(messageId, userEmail)) {
            readRepo.save(
                    GroupMessageReadEntity.builder()
                            .messageId(messageId)
                            .userEmail(userEmail)
                            .readAt(System.currentTimeMillis())
                            .build()
            );
        }

        return readRepo.findByMessageId(messageId);
    }

    /* OFFLINE → ONLINE DELIVERY CATCH-UP */
    @Transactional
    public List<GroupDeliveryAck> markPendingGroupMessagesDelivered(String userEmail) {

        List<GroupMessageEntity> messages =
                messageRepo.findMessagesForUser(userEmail);

        List<GroupDeliveryAck> acks = new ArrayList<>();

        for (GroupMessageEntity msg : messages) {
            if (!deliveryRepo.existsByMessageIdAndUserEmail(
                    msg.getMessageId(), userEmail)) {

                deliveryRepo.save(
                        GroupMessageDeliveryEntity.builder()
                                .messageId(msg.getMessageId())
                                .userEmail(userEmail)
                                .deliveredAt(System.currentTimeMillis())
                                .build()
                );

                acks.add(new GroupDeliveryAck(
                        msg.getGroup().getId(),
                        msg.getMessageId(),
                        userEmail
                ));
            }
        }
        return acks;
    }

    public List<GroupMessageDeliveryEntity> getDeliveries(String messageId) {
        return deliveryRepo.findByMessageId(messageId);
    }

    public List<GroupMessageReadEntity> getReads(String messageId) {
        return readRepo.findByMessageId(messageId);
    }

    public record GroupDeliveryAck(Long groupId, String messageId, String user) {}
}
