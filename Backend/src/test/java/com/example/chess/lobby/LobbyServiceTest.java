package com.example.chess.lobby;

import com.example.chess.common.Dto;
import com.example.chess.game.GameService;
import com.example.chess.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LobbyServiceTest {

    @Mock
    private SimpMessagingTemplate msg;

    @Mock
    private GameService games;

    private LobbyService lobby;

    @BeforeEach
    void setUp() {
        lobby = new LobbyService(msg, games);
    }

    @Test
    void connectBroadcastsOnline() {
        // Given
        var userA = User.builder().id(1L).displayName("Alice").email("a@a.com").build();
        var userB = User.builder().id(2L).displayName("Bob").email("b@b.com").build();

        // When
        lobby.onConnect("session1", userA);
        lobby.onConnect("session2", userB);

        // Then
        verify(msg, atLeastOnce()).convertAndSend(eq("/topic/online-users"), any(Dto.OnlineUsersDto.class));
        assertThat(lobby.currentOnline()).hasSize(2);
        assertThat(lobby.currentOnline().get(0).displayName()).isEqualTo("Alice");
        assertThat(lobby.currentOnline().get(1).displayName()).isEqualTo("Bob");
    }

    @Test
    void disconnect_removesUserFromOnline() {
        // Given
        var user = User.builder().id(1L).displayName("Alice").email("a@a.com").build();
        lobby.onConnect("session1", user);

        // When
        lobby.onDisconnect("session1");

        // Then
        assertThat(lobby.currentOnline()).isEmpty();
        verify(msg, atLeast(2)).convertAndSend(eq("/topic/online-users"), any(Dto.OnlineUsersDto.class));
    }

    @Test
    void disconnect_keepsUserOnlineIfMultipleSessions() {
        // Given
        var user = User.builder().id(1L).displayName("Alice").email("a@a.com").build();
        lobby.onConnect("session1", user);
        lobby.onConnect("session2", user); // Same user, different session

        // When
        lobby.onDisconnect("session1");

        // Then
        assertThat(lobby.currentOnline()).hasSize(1);
        assertThat(lobby.currentOnline().get(0).id()).isEqualTo(1L);
    }

    @Test
    void sendInvite_success_sendsNotification() {
        // Given
        var userA = User.builder().id(1L).displayName("Alice").email("a@a.com").build();
        var userB = User.builder().id(2L).displayName("Bob").email("b@b.com").build();
        lobby.onConnect("session1", userA);
        lobby.onConnect("session2", userB);

        // When
        long inviteId = lobby.sendInvite(1L, 2L);

        // Then
        assertThat(inviteId).isPositive();
        verify(msg).convertAndSendToUser(eq("2"), eq("/queue/invitations"), any(Dto.InviteNotification.class));
    }

    @Test
    void sendInvite_selfInvite_throws() {
        // Given
        var user = User.builder().id(1L).displayName("Alice").email("a@a.com").build();
        lobby.onConnect("session1", user);

        // When & Then
        assertThatThrownBy(() -> lobby.sendInvite(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Self invite not allowed");

        verify(msg, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void sendInvite_offlineRecipient_throws() {
        // Given
        var user = User.builder().id(1L).displayName("Alice").email("a@a.com").build();
        lobby.onConnect("session1", user);

        // When & Then
        assertThatThrownBy(() -> lobby.sendInvite(1L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Recipient is offline");

        verify(msg, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void inviteFlow_accept_createsGameAndDMsBoth() {
        // Given
        var userA = User.builder().id(1L).displayName("Alice").email("a@a.com").build();
        var userB = User.builder().id(2L).displayName("Bob").email("b@b.com").build();
        lobby.onConnect("session1", userA);
        lobby.onConnect("session2", userB);

        long inviteId = lobby.sendInvite(1L, 2L);

        var gameCreated = new Dto.GameCreated("game-123", 1L, 2L);
        when(games.createGameDto(1L, 2L)).thenReturn(gameCreated);

        // When
        lobby.replyInvite(inviteId, true, 2L);

        // Then
        verify(games).createGameDto(1L, 2L);
        verify(msg).convertAndSendToUser(eq("1"), eq("/queue/game-created"), eq(gameCreated));
        verify(msg).convertAndSendToUser(eq("2"), eq("/queue/game-created"), eq(gameCreated));
    }

    @Test
    void inviteFlow_decline_doesNotCreateGame() {
        // Given
        var userA = User.builder().id(1L).displayName("Alice").email("a@a.com").build();
        var userB = User.builder().id(2L).displayName("Bob").email("b@b.com").build();
        lobby.onConnect("session1", userA);
        lobby.onConnect("session2", userB);

        long inviteId = lobby.sendInvite(1L, 2L);

        // When
        lobby.replyInvite(inviteId, false, 2L);

        // Then
        verify(games, never()).createGameDto(anyLong(), anyLong());
        verify(msg, never()).convertAndSendToUser(anyString(), eq("/queue/game-created"), any());
    }

    @Test
    void replyInvite_invalidInviteId_throws() {
        // Given
        var user = User.builder().id(1L).displayName("Alice").email("a@a.com").build();
        lobby.onConnect("session1", user);

        // When & Then
        assertThatThrownBy(() -> lobby.replyInvite(999L, true, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid invitation");
    }

    @Test
    void replyInvite_wrongReplier_throws() {
        // Given
        var userA = User.builder().id(1L).displayName("Alice").email("a@a.com").build();
        var userB = User.builder().id(2L).displayName("Bob").email("b@b.com").build();
        var userC = User.builder().id(3L).displayName("Charlie").email("c@c.com").build();
        lobby.onConnect("session1", userA);
        lobby.onConnect("session2", userB);
        lobby.onConnect("session3", userC);

        long inviteId = lobby.sendInvite(1L, 2L);

        // When & Then - User C trying to reply to invite meant for User B
        assertThatThrownBy(() -> lobby.replyInvite(inviteId, true, 3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid invitation");
    }

    @Test
    void currentOnline_returnsSortedByDisplayName() {
        // Given
        var userZ = User.builder().id(1L).displayName("Zara").email("z@z.com").build();
        var userA = User.builder().id(2L).displayName("Alice").email("a@a.com").build();
        var userB = User.builder().id(3L).displayName("Bob").email("b@b.com").build();

        // When
        lobby.onConnect("session1", userZ);
        lobby.onConnect("session2", userA);
        lobby.onConnect("session3", userB);

        // Then
        var online = lobby.currentOnline();
        assertThat(online).hasSize(3);
        assertThat(online.get(0).displayName()).isEqualTo("Alice");
        assertThat(online.get(1).displayName()).isEqualTo("Bob");
        assertThat(online.get(2).displayName()).isEqualTo("Zara");
    }
}