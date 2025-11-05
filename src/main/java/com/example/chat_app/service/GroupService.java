package com.example.chat_app.service;

import com.example.chat_app.model.Group;
import com.example.chat_app.model.GroupMember;
import com.example.chat_app.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;

    @Transactional
    public Group createGroup(String name, String avatar, String description, String createdBy, List<String> initialMembers) {

        Group g = Group.builder()
                .name(name)
                .avatarUrl(avatar)
                .description(description)
                .createdBy(createdBy)
                .build();

        // creator becomes ADMIN — memberEmail must be the user's email (lowercased)
        GroupMember creatorMember = GroupMember.builder()
                .memberEmail(createdBy.toLowerCase())
                .role("ADMIN")
                .build();
        g.addMember(creatorMember);

        if (initialMembers != null) {
            for (String m : initialMembers) {
                String email = m.toLowerCase();
                if (email.equals(createdBy.toLowerCase())) continue;
                GroupMember gm = GroupMember.builder()
                        .memberEmail(email)
                        .role("MEMBER")
                        .build();
                g.addMember(gm);
            }
        }

        // Persist immediately and return saved instance
        Group saved = groupRepository.saveAndFlush(g);
        return saved;
    }


    public List<Group> listGroupsForUser(String email) {
        return groupRepository.findGroupsByMemberEmail(email.toLowerCase());
    }

    public Optional<Group> getGroup(Long groupId) {
        return groupRepository.findById(groupId);
    }



    @Transactional
    public Group addMember(Long groupId, String actorEmail, String newMemberEmail, String role) {
        Group g = groupRepository.findById(groupId).orElseThrow(() -> new IllegalArgumentException("Group not found"));
        // only allow admins to add members (MVP)
        boolean isAdmin = g.getMembers().stream().anyMatch(m -> m.getMemberEmail().equalsIgnoreCase(actorEmail) && "ADMIN".equalsIgnoreCase(m.getRole()));
        if (!isAdmin) throw new SecurityException("Forbidden");

        // check if already member
        boolean exists = g.getMembers().stream().anyMatch(m -> m.getMemberEmail().equalsIgnoreCase(newMemberEmail));
        if (exists) return g;

        GroupMember gm = GroupMember.builder()
                .memberEmail(newMemberEmail.toLowerCase())
                .role(role == null ? "MEMBER" : role.toUpperCase())
                .build();
        g.addMember(gm);
        return groupRepository.save(g);
    }

    @Transactional
    public Group removeMember(Long groupId, String actorEmail, String targetEmail) {
        Group g = groupRepository.findById(groupId).orElseThrow(() -> new IllegalArgumentException("Group not found"));

        boolean isAdmin = g.getMembers().stream().anyMatch(m -> m.getMemberEmail().equalsIgnoreCase(actorEmail) && "ADMIN".equalsIgnoreCase(m.getRole()));
        boolean selfLeaving = actorEmail.equalsIgnoreCase(targetEmail);
        if (!isAdmin && !selfLeaving) throw new SecurityException("Forbidden");

        g.getMembers().removeIf(m -> m.getMemberEmail().equalsIgnoreCase(targetEmail));
        return groupRepository.save(g);
    }

    @Transactional
    public Group changeMemberRole(Long groupId, String actorEmail, String targetEmail, String newRole) {
        Group g = groupRepository.findById(groupId).orElseThrow(() -> new IllegalArgumentException("Group not found"));

        boolean isAdmin = g.getMembers().stream().anyMatch(m -> m.getMemberEmail().equalsIgnoreCase(actorEmail) && "ADMIN".equalsIgnoreCase(m.getRole()));
        if (!isAdmin) throw new SecurityException("Forbidden");

        g.getMembers().stream()
                .filter(m -> m.getMemberEmail().equalsIgnoreCase(targetEmail))
                .forEach(m -> m.setRole(newRole.toUpperCase()));

        return groupRepository.save(g);
    }
}
