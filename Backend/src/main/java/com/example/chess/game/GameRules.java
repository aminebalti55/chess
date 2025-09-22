package com.example.chess.game;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class GameRules {

    public void validateSquares(String from, String to) {
        if (!from.matches("^[a-h][1-8]$") || !to.matches("^[a-h][1-8]$") || from.equals(to)) {
            throw new IllegalArgumentException("Invalid squares");
        }
    }

    public boolean isWhitesTurn(int nextMoveNumber) {
        return nextMoveNumber % 2 == 1;
    }

    public void validateTurn(Game game, Long userId, int nextMoveNumber) {
        boolean whiteToMove = isWhitesTurn(nextMoveNumber);
        if (whiteToMove && !userId.equals(game.getWhitePlayerId())) {
            throw new IllegalStateException("Not your turn");
        }
        if (!whiteToMove && !userId.equals(game.getBlackPlayerId())) {
            throw new IllegalStateException("Not your turn");
        }
    }

    public void validateParticipant(Game game, Long userId) {
        if (!userId.equals(game.getWhitePlayerId()) && !userId.equals(game.getBlackPlayerId())) {
            throw new AccessDeniedException("Not a participant");
        }
    }
}