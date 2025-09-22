package com.example.chess.config;

import com.example.chess.auth.JwtService;
import com.example.chess.user.User;
import com.example.chess.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String raw = Optional.ofNullable(accessor.getFirstNativeHeader("Authorization"))
                    .orElse(accessor.getFirstNativeHeader("authorization"));

            if (raw == null || raw.isBlank()) {
                log.warn("WS CONNECT without Authorization header");
                return message; // let handshake proceed if you want anonymous WS
            }

            String token = raw.startsWith("Bearer ") ? raw.substring(7) : raw;
            Long userId = jwtService.validateAndExtractUserId(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("User not found for WS"));

            // Create Authentication where:
            // - getPrincipal() returns the User entity (for LobbyController presence logic)
            // - getName() returns the string userId (for Spring user-destination routing)
            Authentication auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()) {
                @Override
                public String getName() {
                    // Used by Spring's user-destination routing for /user/** deliveries
                    return String.valueOf(user.getId());
                }
            };

            accessor.setUser(auth); // CRITICAL: attach principal to STOMP session

            log.info("WS CONNECT authenticated - userId={}, email={}, authName={}",
                    user.getId(), user.getEmail(), auth.getName());

            // Return a message that carries the mutated headers
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        // For other frames, nothing special; principal flows from CONNECT.
        return message;
    }
}