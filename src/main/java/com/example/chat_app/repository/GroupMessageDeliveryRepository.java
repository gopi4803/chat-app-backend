package com.example.chat_app.repository;

import com.example.chat_app.model.GroupMessageDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMessageDeliveryRepository
        extends JpaRepository<GroupMessageDeliveryEntity, Long> {

    boolean existsByMessageIdAndUserEmail(String messageId, String userEmail);

    List<GroupMessageDeliveryEntity> findByMessageId(String messageId);
}
