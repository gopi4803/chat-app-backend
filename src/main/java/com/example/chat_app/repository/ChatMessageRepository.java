package com.example.chat_app.repository;

import com.example.chat_app.model.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    //  Used for incremental sync
    @Query("""
        SELECT c FROM ChatMessageEntity c
        WHERE (LOWER(c.fromUser) = LOWER(:email) OR LOWER(c.toUser) = LOWER(:email))
        AND c.timestamp > :since
        ORDER BY c.timestamp ASC
    """)
    List<ChatMessageEntity> findMessagesAfter(
            @Param("email") String email,
            @Param("since") long since
    );

    //  Used for one-to-one chat history
    @Query("""
        SELECT c FROM ChatMessageEntity c
        WHERE (LOWER(c.fromUser) = LOWER(:me) AND LOWER(c.toUser) = LOWER(:other))
           OR (LOWER(c.fromUser) = LOWER(:other) AND LOWER(c.toUser) = LOWER(:me))
        ORDER BY c.timestamp ASC
    """)
    List<ChatMessageEntity> findChatHistory(
            @Param("me") String me,
            @Param("other") String other
    );

    //  Used for conversation list (latest message per user)
    @Query("""
        SELECT c FROM ChatMessageEntity c
        WHERE LOWER(c.fromUser) = LOWER(:email) OR LOWER(c.toUser) = LOWER(:email)
        ORDER BY c.timestamp ASC
    """)
    List<ChatMessageEntity> findAllMessagesOfUser(@Param("email") String email);
}
