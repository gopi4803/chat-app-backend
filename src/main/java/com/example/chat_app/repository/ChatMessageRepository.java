package com.example.chat_app.repository;

import com.example.chat_app.model.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

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

    @Query("""
        SELECT c FROM ChatMessageEntity c
        WHERE (LOWER(c.fromUser) = LOWER(:me) AND LOWER(c.toUser) = LOWER(:other))
           OR (LOWER(c.fromUser) = LOWER(:other) AND LOWER(c.toUser) = LOWER(:me))
        ORDER BY c.timestamp ASC
    """)
    List<ChatMessageEntity> findChatHistory(@Param("me") String me, @Param("other") String other);

    @Query("""
        SELECT c FROM ChatMessageEntity c
        WHERE LOWER(c.fromUser) = LOWER(:email) OR LOWER(c.toUser) = LOWER(:email)
        ORDER BY c.timestamp ASC
    """)
    List<ChatMessageEntity> findAllMessagesOfUser(@Param("email") String email);

    @Query("""
    SELECT c FROM ChatMessageEntity c
    WHERE LOWER(c.fromUser) = LOWER(:sender)
      AND LOWER(c.toUser) = LOWER(:reader)
      AND c.readAt IS NULL
    """)
    List<ChatMessageEntity> findUnreadMessages(@Param("sender") String sender, @Param("reader") String reader);

    Optional<ChatMessageEntity> findByMessageId(String messageId);
}
