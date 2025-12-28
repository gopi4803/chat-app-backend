package com.example.chat_app.repository;

import com.example.chat_app.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findById(Long id);

    boolean existsByNameAndCreatedByIgnoreCase(String name, String createdBy);

    @Query("SELECT DISTINCT gm.group FROM GroupMember gm WHERE LOWER(gm.memberEmail) = LOWER(:email)")
    List<Group> findGroupsByMemberEmail(@Param("email") String email);
}
