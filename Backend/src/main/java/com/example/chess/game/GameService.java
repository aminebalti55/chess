package com.example.chess.game;

import com.example.chess.common.Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository games;
    private final MoveRepository moves;
    private final GameRules rules;

    @Transactional
    public Game createGame(Long userA, Long userB) {
        // First param is white player, second is black
        Game game = Game.builder()
                .whitePlayerId(userA)
                .blackPlayerId(userB)
                .status(GameStatus.STARTED) // move straight to STARTED after accept
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return games.save(game);
    }

    @Transactional(readOnly = true)
    public List<Move> listMoves(Long gameId) {
        return moves.findByGameIdOrderByMoveNumberAsc(gameId);
    }

    @Transactional(readOnly = true)
    public List<Game> getActiveGamesFor(Long userId) {
        return games.findActiveByUser(userId);
    }

    @Transactional
    public Move recordMove(Long gameId, Long userId, Dto.MoveSend request) {
        Game game = games.findById(gameId)
                .orElseThrow(() -> new NoSuchElementException("Game not found"));

        rules.validateParticipant(game, userId);
        rules.validateSquares(request.from(), request.to());

        int nextMoveNumber = moves.lastMoveNumber(gameId) + 1;
        rules.validateTurn(game, userId, nextMoveNumber);

        // Optional: compute SAN, fenAfter here if you have a helper (skip if short on time)
        Move move = Move.builder()
                .gameId(gameId)
                .moveNumber(nextMoveNumber)
                .fromSquare(request.from())
                .toSquare(request.to())
                .san(request.san())             // may be null; client often sends SAN
                .promotion(request.promotion()) // may be null
                .fenAfter(null)                 // if not computing now
                .playedByUserId(userId)
                .playedAt(Instant.now())
                .build();
        move = moves.save(move);

        // Touch game.updatedAt (and optional lastFen)
        game.setUpdatedAt(Instant.now());
        games.save(game);

        return move;
    }

    // Legacy method for lobby compatibility - returns DTO instead of entity
    @Transactional
    public Dto.GameCreated createGameDto(Long player1Id, Long player2Id) {
        Game game = createGame(player1Id, player2Id);
        return new Dto.GameCreated(
                game.getId().toString(),
                game.getWhitePlayerId(),
                game.getBlackPlayerId()
        );
    }
}