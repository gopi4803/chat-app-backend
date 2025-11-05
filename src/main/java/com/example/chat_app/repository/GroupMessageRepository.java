package com.example.chat_app.repository;

import com.example.chat_app.model.GroupMessageEntity;
import com.example.chat_app.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupMessageRepository extends JpaRepository<GroupMessageEntity, Long> {
    @Query("SELECT m FROM GroupMessageEntity m WHERE m.group.id = :groupId ORDER BY m.timestamp ASC")
    List<GroupMessageEntity> findMessagesByGroupId(@Param("groupId") Long groupId);

    List<GroupMessageEntity> findByGroupOrderByTimestampAsc(Group group);
}
