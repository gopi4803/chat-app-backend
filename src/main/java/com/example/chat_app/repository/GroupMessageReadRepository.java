package com.example.chat_app.repository;

import com.example.chat_app.model.GroupMessageReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMessageReadRepository
        extends JpaRepository<GroupMessageReadEntity, Long> {

    boolean existsByMessageIdAndUserEmail(String messageId, String userEmail);

    List<GroupMessageReadEntity> findByMessageId(String messageId);
}
