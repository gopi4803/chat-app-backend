package com.example.chat_app.service;

import com.example.chat_app.model.Group;
import com.example.chat_app.model.GroupMessageEntity;
import com.example.chat_app.model.User;
import com.example.chat_app.repository.GroupMessageRepository;
import com.example.chat_app.repository.GroupRepository;
import com.example.chat_app.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupMessageService {

    private final GroupMessageRepository messageRepo;
    private final GroupRepository groupRepository;
    private final UserService userService;

    public GroupMessageEntity saveGroupMessage(Long groupId, String sender, String content,
                                               ChatMessage.MessageType type, String messageIdIfProvided, long timestamp) {
        Group g = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        String mid = (messageIdIfProvided != null && !messageIdIfProvided.isBlank())
                ? messageIdIfProvided
                : UUID.randomUUID().toString();

        // Always resolve username from email
        String displayName = userService.getUserByEmail(sender)
                .map(User::getUsername)
                .orElse(sender);

        GroupMessageEntity ent = GroupMessageEntity.fromPayload(mid, g, sender.toLowerCase(), content, timestamp, type);
        ent.setDisplayName(displayName);

        return messageRepo.saveAndFlush(ent); // ensure it persists immediately
    }


    public List<GroupMessageEntity> getGroupMessages(Long groupId) {
        Group g = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        return messageRepo.findByGroupOrderByTimestampAsc(g);
    }

    public void saveSystemMessage(Long groupId, String content) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        GroupMessageEntity msg = new GroupMessageEntity();
        msg.setGroup(group);
        msg.setSender("system");
        msg.setDisplayName("System");
        msg.setContent(content);
        msg.setType(ChatMessage.MessageType.SYSTEM);
        msg.setTimestamp(System.currentTimeMillis());
        msg.setDelivered(true);
        msg.setReadAt(null);

        messageRepo.save(msg);
    }
    @Transactional
    public void updateDisplayName(Long id, String displayName) {
        messageRepo.findById(id).ifPresent(msg -> {
            msg.setDisplayName(displayName);
            messageRepo.save(msg);
        });
    }

}
