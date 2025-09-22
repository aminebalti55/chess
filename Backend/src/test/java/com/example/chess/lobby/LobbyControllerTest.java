package com.example.chess.lobby;

import com.example.chess.common.Dto;
import com.example.chess.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LobbyControllerTest {

    @Mock
    private LobbyService lobby;

    private LobbyController controller;

    @BeforeEach
    void setUp() {
        controller = new LobbyController(lobby);
    }

    @Test
    void inviteSend_delegatesToService() {
        // Given
        var user = User.builder().id(10L).displayName("Alice").email("a@a.com").build();
        Principal principal = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        var request = new Dto.InviteSend(20L);

        // When
        controller.inviteSend(request, principal);

        // Then
        verify(lobby).sendInvite(10L, 20L);
    }

    @Test
    void inviteReply_accept_delegatesToService() {
        // Given
        var user = User.builder().id(20L).displayName("Bob").email("b@b.com").build();
        Principal principal = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        var request = new Dto.InviteReply(123L, true);

        // When
        controller.inviteReply(request, principal);

        // Then
        verify(lobby).replyInvite(123L, true, 20L);
    }

    @Test
    void inviteReply_decline_delegatesToService() {
        // Given
        var user = User.builder().id(20L).displayName("Bob").email("b@b.com").build();
        Principal principal = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        var request = new Dto.InviteReply(456L, false);

        // When
        controller.inviteReply(request, principal);

        // Then
        verify(lobby).replyInvite(456L, false, 20L);
    }

    @Test
    void onWsConnect_withValidUser_delegatesToService() {
        // Given
        var user = User.builder().id(15L).displayName("Charlie").email("c@c.com").build();
        var authentication = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

        // Create proper STOMP headers
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.setSessionId("session123");
        var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        SessionConnectEvent event = new SessionConnectEvent(this, message, authentication);

        // When
        controller.onWsConnect(event);

        // Then
        verify(lobby).onConnect("session123", user);
    }

    @Test
    void onWsConnect_withNonUserPrincipal_doesNotCallService() {
        // Given
        Principal nonUserPrincipal = () -> "some-name";

        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.setSessionId("session123");
        var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        SessionConnectEvent event = new SessionConnectEvent(this, message, nonUserPrincipal);

        // When
        controller.onWsConnect(event);

        // Then
        verify(lobby, never()).onConnect(anyString(), any(User.class));
    }

    @Test
    void onWsConnect_withNullPrincipal_doesNotCallService() {
        // Given
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.setSessionId("session123");
        var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        SessionConnectEvent event = new SessionConnectEvent(this, message, null);

        // When
        controller.onWsConnect(event);

        // Then
        verify(lobby, never()).onConnect(anyString(), any(User.class));
    }

    @Test
    void onWsDisconnect_delegatesToService() {
        // Given
        String sessionId = "session789";
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getSessionId()).thenReturn(sessionId);

        // When
        controller.onWsDisconnect(event);

        // Then
        verify(lobby).onDisconnect(sessionId);
    }

    @Test
    void onWsDisconnect_withNullSessionId_stillCallsService() {
        // Given
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getSessionId()).thenReturn(null);

        // When
        controller.onWsDisconnect(event);

        // Then
        verify(lobby).onDisconnect(null);
    }
}