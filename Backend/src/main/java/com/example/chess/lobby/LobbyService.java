package com.example.chess.lobby;

import com.example.chess.common.Dto;
import com.example.chess.game.GameService;
import com.example.chess.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class LobbyService {

    private final SimpMessagingTemplate messaging;

    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();
    private final Map<Long, Dto.UserLite> online = new ConcurrentHashMap<>();

    // Invite management - using Long IDs to match DTO
    private final AtomicLong inviteIdSequence = new AtomicLong(1);
    private final Map<Long, Invite> invites = new ConcurrentHashMap<>();

    // Lightweight in-memory invite - using Long ID
    private record Invite(Long id, Long fromUserId, Long toUserId, Instant createdAt) {}

    public void onConnect(String sessionId, User user) {
        sessionToUser.put(sessionId, user.getId());
        online.put(user.getId(), new Dto.UserLite(user.getId(), user.getDisplayName()));
        log.info("Online +1: uid={} session={} total={}", user.getId(), sessionId, online.size());
        broadcastOnline();
    }

    public void onDisconnect(String sessionId) {
        Long uid = sessionToUser.remove(sessionId);
        if (uid == null) {
            log.warn("Disconnect for unknown session: {}", sessionId);
            return;
        }
        // If the user has no other active sessions, remove them from online
        if (!sessionToUser.containsValue(uid)) {
            online.remove(uid);
        }
        log.info("Online -1: uid={} session={} total={}", uid, sessionId, online.size());
        broadcastOnline();
    }

    public void broadcastOnline() {
        List<Dto.UserLite> users = online.values().stream()
                .sorted(Comparator.comparing(Dto.UserLite::displayName))
                .toList();
        messaging.convertAndSend("/topic/online-users", new Dto.OnlineUsersDto(users));
        log.info("Broadcasted online users: {}", users.size());
    }

    public List<Dto.UserLite> currentOnline() {
        return online.values().stream()
                .sorted(Comparator.comparing(Dto.UserLite::displayName))
                .toList();
    }

    public Optional<Dto.UserLite> getUserLite(Long userId) {
        return Optional.ofNullable(online.get(userId));
    }

    // ------- INVITES -------

    public Long createInvite(Long fromUserId, Long toUserId) {
        Long id = inviteIdSequence.getAndIncrement();
        invites.put(id, new Invite(id, fromUserId, toUserId, Instant.now()));
        return id;
    }

    public Optional<Invite> findInvite(Long inviteId) {
        return Optional.ofNullable(invites.get(inviteId));
    }

    public void removeInvite(Long inviteId) {
        invites.remove(inviteId);
    }

    // Helper methods to access invite details without exposing the record
    public Long getInviteFromUserId(Long inviteId) {
        return findInvite(inviteId)
                .map(Invite::fromUserId)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found: " + inviteId));
    }

    public Long getInviteToUserId(Long inviteId) {
        return findInvite(inviteId)
                .map(Invite::toUserId)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found: " + inviteId));
    }

    public void sendInviteNotificationToRecipient(Long invitationId, Long toUserId, Dto.UserLite fromUser) {
        messaging.convertAndSendToUser(
                String.valueOf(toUserId), "/queue/invitations",
                new Dto.InviteNotification(invitationId, fromUser)
        );
        log.info("Invite sent id={} toUserId={} from='{}'", invitationId, toUserId, fromUser.displayName());
    }

    public void notifyDeclined(Long invitationId, Long fromUserId, Dto.UserLite byUser) {
        messaging.convertAndSendToUser(
                String.valueOf(fromUserId), "/queue/invitations",
                new Dto.InviteDeclined(String.valueOf(invitationId), byUser)
        );
        log.info("Invite declined id={} by='{}' notified sender={}", invitationId, byUser.displayName(), fromUserId);
    }

    public void notifyGameCreated(Long userId, Dto.GameCreated payload) {
        messaging.convertAndSendToUser(String.valueOf(userId), "/queue/game-created", payload);
    }
}