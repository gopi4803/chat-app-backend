package com.example.chat_app.websocket;

import com.example.chat_app.service.UserDetailsServiceImpl;
import com.example.chat_app.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Intercepts inbound STOMP messages. Validates JWT on CONNECT and sets user principal as email.
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthenticationChannelInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthenticationChannelInterceptor.class);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = firstNonNullHeader(accessor.getNativeHeader("Authorization"));
            String accessToken = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                accessToken = authHeader.substring(7);
            } else {
                String alt = firstNonNullHeader(accessor.getNativeHeader("access_token"));
                if (alt != null) accessToken = alt;
            }

            if (accessToken == null) {
                logger.warn(" WebSocket CONNECT without access token");
                return message;
            }

            try {
                if (!jwtUtils.validateJwtToken(accessToken)) {
                    logger.warn(" Invalid WebSocket token");
                    return message;
                }

                String email = jwtUtils.getEmailFromJwtToken(accessToken);
                if (email == null) {
                    logger.warn(" Token did not contain email");
                    return message;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                //  IMPORTANT: use email string as principal name
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, userDetails.getAuthorities());

                accessor.setUser(authentication);
                logger.info(" WebSocket user authenticated: {}", email);

            } catch (Exception ex) {
                logger.error(" WebSocket authentication failed: {}", ex.getMessage(), ex);
            }
        }
        return message;
    }

    private String firstNonNullHeader(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(0);
    }
}
