package com.example.chess.lobby;

import com.example.chess.common.Dto;
import com.example.chess.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LobbyServiceTest {

    @Mock
    private SimpMessagingTemplate messaging;

    private LobbyService lobbyService;

    @BeforeEach
    void setUp() {
        lobbyService = new LobbyService(messaging);
    }

    @Test
    void onConnect_addsUserToOnlineAndBroadcasts() {
        // Given
        var user = User.builder().id(1L).displayName("Alice").email("a@a.com").build();

        // When
        lobbyService.onConnect("session1", user);

        // Then
        assertThat(lobbyService.currentOnline()).hasSize(1);
        assertThat(lobbyService.currentOnline().get(0).id()).isEqualTo(1L);
        assertThat(lobbyService.currentOnline().get(0).displayName()).isEqualTo("Alice");

        verify(messaging).convertAndSend(eq("/topic/online-users"), any(Dto.OnlineUsersDto.class));
    }

    @Test
    void onConnect_multipleUsers_addsAllAndSortsByDisplayName() {
        // Given
        var userZ = User.builder().id(1L).displayName("Zara").email("z@z.com").build();
        var userA = User.builder().id(2L).displayName("Alice").email("a@a.com").build();
        var userB = User.builder().id(3L).displayName("Bob").email("b@b.com").build();

        // When
        lobbyService.onConnect("session1", userZ);
        lobbyService.onConnect("session2", userA);
        lobbyService.onConnect("session3", userB);

        // Then
        var online = lobbyService.currentOnline();
        assertThat(online).hasSize(3);
        assertThat(online.get(0).displayName()).isEqualTo("Alice");
        assertThat(online.get(1).displayName()).isEqualTo("Bob");
        assertThat(online.get(2).displayName()).isEqualTo("Zara");
    }

    @Test
    void onConnect_sameUserMultipleSessions_showsOnlyOnce() {
        // Given
        var user = User.builder().id(1L).displayName("Alice").email("a@a.com").build();

        // When
        lobbyService.onConnect("session1", user);
        lobbyService.onConnect("session2", user); // Same user, different session

        // Then
        assertThat(lobbyService.currentOnline()).hasSize(1);
        assertThat(lobbyService.currentOnline().get(0).id()).isEqualTo(1L);
    }

    @Test
    void onDisconnect_removesUserFromOnlineWhenLastSession() {
        // Given
        var user = User.builder().id(1L).displayName("Alice").email("a@a.com").build();
        lobbyService.onConnect("session1", user);

        // When
        lobbyService.onDisconnect("session1");

        // Then
        assertThat(lobbyService.currentOnline()).isEmpty();
        verify(messaging, atLeast(2)).convertAndSend(eq("/topic/online-users"), any(Dto.OnlineUsersDto.class));
    }

    @Test
    void onDisconnect_keepsUserOnlineWhenOtherSessionsExist() {
        // Given
        var user = User.builder().id(1L).displayName("Alice").email("a@a.com").build();
        lobbyService.onConnect("session1", user);
        lobbyService.onConnect("session2", user); // Same user, different session

        // When
        lobbyService.onDisconnect("session1");

        // Then
        assertThat(lobbyService.currentOnline()).hasSize(1);
        assertThat(lobbyService.currentOnline().get(0).id()).isEqualTo(1L);
    }

    @Test
    void onDisconnect_unknownSession_doesNotCrash() {
        // When & Then
        assertThatCode(() -> lobbyService.onDisconnect("unknown-session"))
                .doesNotThrowAnyException();

        verify(messaging).convertAndSend(eq("/topic/online-users"), any(Dto.OnlineUsersDto.class));
    }

    @Test
    void broadcastOnline_sendsOnlineUsersToTopic() {
        // Given
        var user1 = User.builder().id(1L).displayName("Alice").email("a@a.com").build();
        var user2 = User.builder().id(2L).displayName("Bob").email("b@b.com").build();
        lobbyService.onConnect("session1", user1);
        lobbyService.onConnect("session2", user2);

        // When
        lobbyService.broadcastOnline();

        // Then
        ArgumentCaptor<Dto.OnlineUsersDto> captor = ArgumentCaptor.forClass(Dto.OnlineUsersDto.class);
        verify(messaging, atLeastOnce()).convertAndSend(eq("/topic/online-users"), captor.capture());

        Dto.OnlineUsersDto captured = captor.getValue();
        assertThat(captured.users()).hasSize(2);
        assertThat(captured.users().get(0).displayName()).isEqualTo("Alice");
        assertThat(captured.users().get(1).displayName()).isEqualTo("Bob");
    }

    @Test
    void getUserLite_existingUser_returnsUserLite() {
        // Given
        var user = User.builder().id(1L).displayName("Alice").email("a@a.com").build();
        lobbyService.onConnect("session1", user);

        // When
        var result = lobbyService.getUserLite(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1L);
        assertThat(result.get().displayName()).isEqualTo("Alice");
    }

    @Test
    void getUserLite_nonExistentUser_returnsEmpty() {
        // When
        var result = lobbyService.getUserLite(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void createInvite_returnsIncrementingId() {
        // When
        Long invite1 = lobbyService.createInvite(1L, 2L);
        Long invite2 = lobbyService.createInvite(3L, 4L);

        // Then
        assertThat(invite1).isPositive();
        assertThat(invite2).isPositive();
        assertThat(invite2).isGreaterThan(invite1);
    }

    @Test
    void findInvite_existingInvite_returnsInvite() {
        // Given
        Long inviteId = lobbyService.createInvite(1L, 2L);

        // When
        var result = lobbyService.findInvite(inviteId);

        // Then
        assertThat(result).isPresent();
    }

    @Test
    void findInvite_nonExistentInvite_returnsEmpty() {
        // When
        var result = lobbyService.findInvite(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void removeInvite_removesInviteFromStorage() {
        // Given
        Long inviteId = lobbyService.createInvite(1L, 2L);
        assertThat(lobbyService.findInvite(inviteId)).isPresent();

        // When
        lobbyService.removeInvite(inviteId);

        // Then
        assertThat(lobbyService.findInvite(inviteId)).isEmpty();
    }

    @Test
    void getInviteFromUserId_existingInvite_returnsFromUserId() {
        // Given
        Long inviteId = lobbyService.createInvite(10L, 20L);

        // When
        Long fromUserId = lobbyService.getInviteFromUserId(inviteId);

        // Then
        assertThat(fromUserId).isEqualTo(10L);
    }

    @Test
    void getInviteFromUserId_nonExistentInvite_throwsException() {
        // When & Then
        assertThatThrownBy(() -> lobbyService.getInviteFromUserId(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invite not found: 999");
    }

    @Test
    void getInviteToUserId_existingInvite_returnsToUserId() {
        // Given
        Long inviteId = lobbyService.createInvite(10L, 20L);

        // When
        Long toUserId = lobbyService.getInviteToUserId(inviteId);

        // Then
        assertThat(toUserId).isEqualTo(20L);
    }

    @Test
    void getInviteToUserId_nonExistentInvite_throwsException() {
        // When & Then
        assertThatThrownBy(() -> lobbyService.getInviteToUserId(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invite not found: 999");
    }

    @Test
    void sendInviteNotificationToRecipient_sendsToUserQueue() {
        // Given
        Long invitationId = 123L;
        Long toUserId = 20L;
        Dto.UserLite fromUser = new Dto.UserLite(10L, "Alice");

        // When
        lobbyService.sendInviteNotificationToRecipient(invitationId, toUserId, fromUser);

        // Then
        ArgumentCaptor<Dto.InviteNotification> captor = ArgumentCaptor.forClass(Dto.InviteNotification.class);
        verify(messaging).convertAndSendToUser(
                eq("20"),
                eq("/queue/invitations"),
                captor.capture()
        );

        Dto.InviteNotification notification = captor.getValue();
        assertThat(notification.invitationId()).isEqualTo(123L);
        assertThat(notification.fromUser().id()).isEqualTo(10L);
        assertThat(notification.fromUser().displayName()).isEqualTo("Alice");
    }

    @Test
    void notifyDeclined_sendsDeclineNotificationToSender() {
        // Given
        Long invitationId = 123L;
        Long fromUserId = 10L;
        Dto.UserLite byUser = new Dto.UserLite(20L, "Bob");

        // When
        lobbyService.notifyDeclined(invitationId, fromUserId, byUser);

        // Then
        ArgumentCaptor<Dto.InviteDeclined> captor = ArgumentCaptor.forClass(Dto.InviteDeclined.class);
        verify(messaging).convertAndSendToUser(
                eq("10"),
                eq("/queue/invitations"),
                captor.capture()
        );

        Dto.InviteDeclined declined = captor.getValue();
        assertThat(declined.invitationId()).isEqualTo("123");
        assertThat(declined.byUser().id()).isEqualTo(20L);
        assertThat(declined.byUser().displayName()).isEqualTo("Bob");
    }

    @Test
    void notifyGameCreated_sendsGameCreatedToUser() {
        // Given
        Long userId = 10L;
        Dto.GameCreated gameCreated = new Dto.GameCreated("game-123", 10L, 20L);

        // When
        lobbyService.notifyGameCreated(userId, gameCreated);

        // Then
        verify(messaging).convertAndSendToUser(
                eq("10"),
                eq("/queue/game-created"),
                eq(gameCreated)
        );
    }

    @Test
    void currentOnline_emptyWhenNoUsers() {
        // When
        var result = lobbyService.currentOnline();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void currentOnline_returnsSortedList() {
        // Given
        var userC = User.builder().id(1L).displayName("Charlie").email("c@c.com").build();
        var userA = User.builder().id(2L).displayName("Alice").email("a@a.com").build();
        var userB = User.builder().id(3L).displayName("Bob").email("b@b.com").build();

        lobbyService.onConnect("session1", userC);
        lobbyService.onConnect("session2", userA);
        lobbyService.onConnect("session3", userB);

        // When
        var result = lobbyService.currentOnline();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).displayName()).isEqualTo("Alice");
        assertThat(result.get(1).displayName()).isEqualTo("Bob");
        assertThat(result.get(2).displayName()).isEqualTo("Charlie");
    }

    @Test
    void inviteWorkflow_createFindAndRemove() {
        // Given
        Long fromUserId = 10L;
        Long toUserId = 20L;

        // When - Create invite
        Long inviteId = lobbyService.createInvite(fromUserId, toUserId);

        // Then - Should exist and have correct details
        assertThat(lobbyService.findInvite(inviteId)).isPresent();
        assertThat(lobbyService.getInviteFromUserId(inviteId)).isEqualTo(fromUserId);
        assertThat(lobbyService.getInviteToUserId(inviteId)).isEqualTo(toUserId);

        // When - Remove invite
        lobbyService.removeInvite(inviteId);

        // Then - Should no longer exist
        assertThat(lobbyService.findInvite(inviteId)).isEmpty();
    }
}