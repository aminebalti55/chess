package com.example.chess.config;

import com.example.chess.auth.JwtService;
import com.example.chess.user.User;
import com.example.chess.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtChannelInterceptorTest {

    @Mock
    private JwtService jwt;

    @Mock
    private UserRepository users;

    private JwtChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new JwtChannelInterceptor(jwt, users);
    }

    @Test
    void connectWithValidToken_setsPrincipal() {
        // Given
        when(jwt.getUserId("valid-token")).thenReturn(5L);
        when(users.findById(5L)).thenReturn(Optional.of(
                User.builder().id(5L).email("test@test.com").displayName("Test User").build()
        ));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer valid-token");
        accessor.setSessionId("session1");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When
        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        // Then
        assertThat(result).isNotNull();
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertThat(resultAccessor.getUser()).isNotNull();
        assertThat(resultAccessor.getUser()).isInstanceOf(Authentication.class);
        assertThat(resultAccessor.getSessionAttributes()).isNotNull();
        assertThat(resultAccessor.getSessionAttributes().get("userId")).isEqualTo(5L);

        verify(jwt).getUserId("valid-token");
        verify(users).findById(5L);
    }

    @Test
    void connectWithoutToken_throws() {
        // Given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("session1");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When & Then
        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Missing Bearer token");

        verify(jwt, never()).getUserId(anyString());
        verify(users, never()).findById(anyLong());
    }

    @Test
    void connectWithInvalidToken_throws() {
        // Given
        when(jwt.getUserId("invalid-token")).thenThrow(new RuntimeException("Invalid token"));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer invalid-token");
        accessor.setSessionId("session1");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When & Then
        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Invalid STOMP CONNECT");

        verify(jwt).getUserId("invalid-token");
        verify(users, never()).findById(anyLong());
    }

    @Test
    void connectWithNonExistentUser_throws() {
        // Given
        when(jwt.getUserId("token")).thenReturn(99L);
        when(users.findById(99L)).thenReturn(Optional.empty());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer token");
        accessor.setSessionId("session1");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When & Then
        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User not found");

        verify(jwt).getUserId("token");
        verify(users).findById(99L);
    }

    @Test
    void nonConnectMessage_passesThrough() {
        // Given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionId("session1");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When
        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        // Then
        assertThat(result).isSameAs(message);
        verify(jwt, never()).getUserId(anyString());
        verify(users, never()).findById(anyLong());
    }

    @Test
    void connectWithDifferentHeaderNames_works() {
        // Given
        when(jwt.getUserId("token")).thenReturn(1L);
        when(users.findById(1L)).thenReturn(Optional.of(
                User.builder().id(1L).email("test@test.com").displayName("Test").build()
        ));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("auth", "Bearer token"); // Using 'auth' instead of 'Authorization'
        accessor.setSessionId("session1");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When
        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        // Then
        assertThat(result).isNotNull();
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertThat(resultAccessor.getUser()).isNotNull();
        assertThat(resultAccessor.getSessionAttributes()).isNotNull();
        assertThat(resultAccessor.getSessionAttributes().get("userId")).isEqualTo(1L);

        verify(jwt).getUserId("token");
        verify(users).findById(1L);
    }

    @Test
    void connectWithLowercaseAuthorizationHeader_works() {
        // Given
        when(jwt.getUserId("token")).thenReturn(2L);
        when(users.findById(2L)).thenReturn(Optional.of(
                User.builder().id(2L).email("test2@test.com").displayName("Test2").build()
        ));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("authorization", "Bearer token"); // lowercase
        accessor.setSessionId("session1");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When
        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        // Then
        assertThat(result).isNotNull();
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertThat(resultAccessor.getUser()).isNotNull();
        assertThat(resultAccessor.getSessionAttributes()).isNotNull();
        assertThat(resultAccessor.getSessionAttributes().get("userId")).isEqualTo(2L);

        verify(jwt).getUserId("token");
        verify(users).findById(2L);
    }

    @Test
    void connectWithMalformedBearerToken_throws() {
        // Given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "NotBearer token123");
        accessor.setSessionId("session1");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When & Then
        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Missing Bearer token");

        verify(jwt, never()).getUserId(anyString());
        verify(users, never()).findById(anyLong());
    }
}