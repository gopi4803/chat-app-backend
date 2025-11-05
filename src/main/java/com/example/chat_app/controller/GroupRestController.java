package com.example.chat_app.controller;

import com.example.chat_app.model.Group;
import com.example.chat_app.model.GroupMember;
import com.example.chat_app.model.User;
import com.example.chat_app.security.JwtUtils;
import com.example.chat_app.service.GroupService;
import com.example.chat_app.service.GroupMessageService;
import com.example.chat_app.model.GroupMessageEntity;
import com.example.chat_app.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class GroupRestController {

    private final GroupService groupService;
    private final GroupMessageService groupMessageService;
    private final JwtUtils jwtUtils;
    private final HttpServletRequest request;
    private final UserService userService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    private final Logger logger = (Logger) LoggerFactory.getLogger(GroupRestController.class);

    private String resolveEmail(Authentication auth) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String emailFromToken = jwtUtils.getEmailFromJwtToken(token);
                if (emailFromToken != null && !emailFromToken.isBlank()) {
                    return emailFromToken.toLowerCase();
                }
            } catch (Exception ex) {
                logger.debug("Failed to extract email from JWT: {}", ex.getMessage());
            }
        }

        if (auth != null && auth.getName() != null) {
            return auth.getName().toLowerCase();
        }

        return null;
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> payload, Authentication auth) {
        String creator = resolveEmail(auth);
        if (creator == null) return ResponseEntity.status(401).build();
        String name = (String) payload.get("name");
        String avatar = (String) payload.get("avatar");
        String description = (String) payload.get("description");
        List<String> members = (List<String>) payload.getOrDefault("members", Collections.emptyList());
        if (name == null || name.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "name is required"));

        try {
            Group g = groupService.createGroup(name.trim(), avatar, description, creator, members);

            // Build full members list (for broadcast)
            List<Map<String, Object>> membersList = g.getMembers().stream()
                    .map(mem -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("email", mem.getMemberEmail());
                        m.put("role", mem.getRole());
                        m.put("joinedAt", mem.getJoinedAt());
                        return m;
                    })
                    .collect(Collectors.toList());

            List<String> allEmails = g.getMembers().stream()
                    .map(GroupMember::getMemberEmail)
                    .toList();

            // WebSocket event for all users
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", "GROUP_CREATED");
            event.put("groupId", g.getId());
            event.put("name", g.getName());
            event.put("avatar", g.getAvatarUrl());
            event.put("createdBy", creator);
            event.put("members", membersList);

            for (String member : allEmails) {
                simpMessagingTemplate.convertAndSendToUser(
                        member, "/queue/group.events", event);
            }

            // Instead of sending group system message to /topic immediately,
            // send it to each user's private queue so everyone sees it,
            // even if they haven't subscribed yet.
            Map<String, Object> sysMessage = new LinkedHashMap<>();
            sysMessage.put("type", "GROUP_SYSTEM");
            sysMessage.put("groupId", g.getId());

            //  Fetch creator's username from UserService (fallback to email)
            String creatorName = userService.getUserByEmail(creator)
                    .map(User::getUsername)
                    .orElse(creator);

            //  Fetch usernames of members
            List<String> readableMembers = g.getMembers().stream()
                    .map(GroupMember::getMemberEmail)
                    .filter(email -> !email.equalsIgnoreCase(creator))
                    .map(email -> userService.getUserByEmail(email)
                            .map(User::getUsername)
                            .orElse(email))
                    .collect(Collectors.toList());

            //  Build message text using usernames
            String messageText = creatorName + " created the group \"" + g.getName() + "\".";
            if (!readableMembers.isEmpty()) {
                messageText += " Added: " + String.join(", ", readableMembers);
            }
            sysMessage.put("content", messageText);
            sysMessage.put("timestamp", System.currentTimeMillis());
            for (String member : allEmails) {
                simpMessagingTemplate.convertAndSendToUser(
                        member, "/queue/group.system", sysMessage);
            }

            // HTTP response (for the creator)
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("id", g.getId());
            res.put("name", g.getName());
            res.put("avatar", g.getAvatarUrl());
            res.put("createdBy", g.getCreatedBy());
            res.put("members", membersList);

            groupMessageService.saveSystemMessage(g.getId(), messageText);

            return ResponseEntity.ok(res);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create group", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Unexpected error"));
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listMyGroups(Authentication auth) {
        String me = resolveEmail(auth);
        if (me == null) return ResponseEntity.status(401).build();

        logger.info("listMyGroups: user={}", me);
        List<Group> groups = groupService.listGroupsForUser(me);

        List<Map<String, Object>> res = groups.stream().map(g -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId());
            m.put("name", g.getName());
            m.put("avatar", g.getAvatarUrl());
            m.put("createdBy", g.getCreatedBy());
            List<Map<String, Object>> membersList = g.getMembers().stream().map(mem -> {
                Map<String, Object> memberMap = new LinkedHashMap<>();
                memberMap.put("email", mem.getMemberEmail());
                memberMap.put("role", mem.getRole());
                memberMap.put("joinedAt", mem.getJoinedAt());
                return memberMap;
            }).collect(Collectors.toList());
            m.put("members", membersList);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<?> addMember(@PathVariable Long id, @RequestBody Map<String,String> body, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        String me = resolveEmail(auth);
        String member = Optional.ofNullable(body.get("email")).orElse("").toLowerCase();
        String role = Optional.ofNullable(body.get("role")).orElse("MEMBER");
        try {
            groupService.addMember(id, me, member, role); // if you have addMember on service
            return ResponseEntity.ok(Map.of("success", true));
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}/members/{email}")
    public ResponseEntity<?> removeMember(@PathVariable Long id, @PathVariable String email, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        String me = auth.getName().toLowerCase();
        try {
            Group updated = groupService.removeMember(id, me, email.toLowerCase());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getGroupMessages(@PathVariable Long id, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        try {
            List<GroupMessageEntity> msgs = groupMessageService.getGroupMessages(id);

            List<Map<String, Object>> out = msgs.stream().map(m -> {
                Map<String, Object> msgMap = new LinkedHashMap<>();
                msgMap.put("messageId", m.getMessageId());
                msgMap.put("groupId", m.getGroup().getId());

                //  Determine display name correctly
                String senderDisplay = null;

                if (m.getDisplayName() != null && !m.getDisplayName().isBlank()) {
                    senderDisplay = m.getDisplayName();
                } else if (m.getSender() != null && !m.getSender().equalsIgnoreCase("system")) {
                    // Try fetching username if missing
                    senderDisplay = userService.getUserByEmail(m.getSender())
                            .map(User::getUsername)
                            .orElse(m.getSender());
                    // Patch DB if displayName missing (so next reload is instant)
                    try {
                        m.setDisplayName(senderDisplay);
                        groupMessageService.updateDisplayName(m.getId(), senderDisplay);
                    } catch (Exception ignored) {}
                } else if ("system".equalsIgnoreCase(m.getSender())) {
                    senderDisplay = "System";
                }

                msgMap.put("sender", m.getSender());
                msgMap.put("senderName", senderDisplay);
                msgMap.put("content", m.getContent());
                msgMap.put("timestamp", m.getTimestamp());
                msgMap.put("type", m.getType());
                msgMap.put("delivered", m.isDelivered());
                msgMap.put("readAt", m.getReadAt());
                return msgMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(out);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

}
