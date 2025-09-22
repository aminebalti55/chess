package com.example.chess.lobby;

import com.example.chess.common.Dto;
import com.example.chess.game.Game;
import com.example.chess.game.GameService;
import com.example.chess.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class LobbyController {

    private final LobbyService lobby;
    private final SimpMessagingTemplate messaging;
    private final GameService gameService;

    // ====== CONNECT / DISCONNECT / SUBSCRIBE ======

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        Principal principal = event.getUser();

        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof User user) {
            log.info("Connected: userId={} session={}", user.getId(), sessionId);
            lobby.onConnect(sessionId, user);
            lobby.broadcastOnline();
        } else {
            log.warn("Connected without principal: session={}", sessionId);
        }
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        lobby.onDisconnect(event.getSessionId());
        lobby.broadcastOnline();
    }

    @EventListener
    public void onSubscribed(SessionSubscribeEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headers.getDestination();
        if ("/topic/online-users".equals(destination)) {
            lobby.broadcastOnline(); // snapshot on subscribe
        }
    }

    @MessageMapping("presence.request")
    public void presenceRequest() {
        lobby.broadcastOnline(); // explicit snapshot request from clients
    }

    // ====== INVITES ======

    @MessageMapping("invite.send")
    public void inviteSend(Dto.InviteSend req, Principal principal) {
        if (!(principal instanceof Authentication auth) || !(auth.getPrincipal() instanceof User me)) {
            throw new IllegalStateException("Unauthorized invite");
        }
        if (req == null || req.toUserId() == null) {
            throw new IllegalArgumentException("toUserId is required");
        }
        if (req.toUserId().equals(me.getId())) {
            throw new IllegalArgumentException("Cannot invite yourself");
        }

        Long inviteId = lobby.createInvite(me.getId(), req.toUserId());
        Dto.UserLite fromLite = new Dto.UserLite(me.getId(), me.getDisplayName());

        // Push to recipient's personal queue
        lobby.sendInviteNotificationToRecipient(inviteId, req.toUserId(), fromLite);
        log.info("Invite created id={} from={} to={}", inviteId, me.getId(), req.toUserId());
    }

    @MessageMapping("invite.reply")
    public void inviteReply(Dto.InviteReply req, Principal principal) {
        if (!(principal instanceof Authentication auth) || !(auth.getPrincipal() instanceof User me)) {
            throw new IllegalStateException("Unauthorized invite reply");
        }
        if (req == null || req.invitationId() == null) {
            throw new IllegalArgumentException("invitationId is required");
        }

        var inviteOpt = lobby.findInvite(req.invitationId());
        if (inviteOpt.isEmpty()) {
            log.warn("Invite not found id={}", req.invitationId());
            return;
        }

        // Get the invite details through service methods
        Long fromUserId = lobby.getInviteFromUserId(req.invitationId());
        Long toUserId = lobby.getInviteToUserId(req.invitationId());

        // Ensure this user is the recipient
        if (!toUserId.equals(me.getId())) {
            throw new IllegalStateException("Not the invite recipient");
        }

        // Remove invite once processed
        lobby.removeInvite(req.invitationId());

        if (!req.accept()) {
            // notify sender that it was declined
            lobby.notifyDeclined(req.invitationId(), fromUserId, new Dto.UserLite(me.getId(), me.getDisplayName()));
            return;
        }

        // ACCEPTED → create a game and notify both players
        // Sender is white, recipient is black
        Game game = gameService.createGame(fromUserId, toUserId);

        var created = new Dto.GameCreated(game.getId().toString(), game.getWhitePlayerId(), game.getBlackPlayerId());
        lobby.notifyGameCreated(fromUserId, created);
        lobby.notifyGameCreated(toUserId, created);
        log.info("Game created id={} white={} black={}", game.getId(), game.getWhitePlayerId(), game.getBlackPlayerId());
    }

    @MessageExceptionHandler
    public void handleWsError(Throwable t) {
        log.error("WS error", t);
    }
}