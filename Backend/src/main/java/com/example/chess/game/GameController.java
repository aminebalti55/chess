package com.example.chess.game;

import com.example.chess.common.Dto;
import com.example.chess.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@Controller
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService service;
    private final SimpMessagingTemplate msg;

    @GetMapping("/active")
    public List<Dto.ActiveGameDto> active(@AuthenticationPrincipal User me) {
        return service.getActiveGamesFor(me.getId()).stream()
                .map(game -> new Dto.ActiveGameDto(
                        game.getId(),
                        game.getWhitePlayerId().equals(me.getId()),
                        game.getStatus().name(),
                        game.getLastFen()
                ))
                .toList();
    }

    @GetMapping("/{id}/moves")
    public List<Dto.MoveRecord> moves(@PathVariable Long id, @AuthenticationPrincipal User me) {
        // Optionally verify participant before returning
        return service.listMoves(id).stream()
                .map(move -> new Dto.MoveRecord(
                        move.getMoveNumber(),
                        move.getFromSquare(),
                        move.getToSquare(),
                        move.getSan(),
                        move.getPromotion(),
                        move.getPlayedByUserId(),
                        move.getPlayedAt(),
                        move.getFenAfter()
                ))
                .toList();
    }

    // === WebSocket mapping ===
    @MessageMapping("/games/{id}/move")
    public void submitMove(@DestinationVariable("id") Long gameId,
                           @Valid Dto.MoveSend request,
                           Principal principal) {
        var authentication = (Authentication) principal;
        var user = (User) authentication.getPrincipal();

        Move move = service.recordMove(gameId, user.getId(), request);

        var payload = new Dto.MoveBroadcast(
                move.getMoveNumber(),
                move.getFromSquare(),
                move.getToSquare(),
                move.getSan(),
                move.getPlayedByUserId(),
                move.getPlayedAt(),
                move.getPromotion()
        );

        msg.convertAndSend("/topic/games/" + gameId, payload);
    }
}