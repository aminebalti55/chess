package com.example.chess.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.*;

class GameRulesTest {

    private GameRules rules;

    @BeforeEach
    void setUp() {
        rules = new GameRules();
    }

    @Test
    void validateSquares_validSquares_passes() {
        // Should not throw for valid squares
        assertThatNoException().isThrownBy(() -> rules.validateSquares("e2", "e4"));
        assertThatNoException().isThrownBy(() -> rules.validateSquares("a1", "h8"));
        assertThatNoException().isThrownBy(() -> rules.validateSquares("d7", "d5"));
    }

    @Test
    void validateSquares_invalidFile_throws() {
        assertThatThrownBy(() -> rules.validateSquares("i2", "e4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid squares");

        assertThatThrownBy(() -> rules.validateSquares("e2", "z4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid squares");
    }

    @Test
    void validateSquares_invalidRank_throws() {
        assertThatThrownBy(() -> rules.validateSquares("e0", "e4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid squares");

        assertThatThrownBy(() -> rules.validateSquares("e2", "e9"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid squares");
    }

    @Test
    void validateSquares_sameSquare_throws() {
        assertThatThrownBy(() -> rules.validateSquares("e4", "e4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid squares");
    }

    @Test
    void validateSquares_invalidFormat_throws() {
        assertThatThrownBy(() -> rules.validateSquares("e", "e4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid squares");

        assertThatThrownBy(() -> rules.validateSquares("e22", "e4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid squares");

        assertThatThrownBy(() -> rules.validateSquares("", "e4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid squares");
    }

    @Test
    void isWhitesTurn_oddMoveNumbers_returnsTrue() {
        assertThat(rules.isWhitesTurn(1)).isTrue();
        assertThat(rules.isWhitesTurn(3)).isTrue();
        assertThat(rules.isWhitesTurn(5)).isTrue();
        assertThat(rules.isWhitesTurn(99)).isTrue();
    }

    @Test
    void isWhitesTurn_evenMoveNumbers_returnsFalse() {
        assertThat(rules.isWhitesTurn(2)).isFalse();
        assertThat(rules.isWhitesTurn(4)).isFalse();
        assertThat(rules.isWhitesTurn(6)).isFalse();
        assertThat(rules.isWhitesTurn(100)).isFalse();
    }

    @Test
    void validateTurn_whitePlayerOnOddMove_passes() {
        Game game = Game.builder()
                .whitePlayerId(1L)
                .blackPlayerId(2L)
                .build();

        // White's turn (odd move numbers)
        assertThatNoException().isThrownBy(() -> rules.validateTurn(game, 1L, 1));
        assertThatNoException().isThrownBy(() -> rules.validateTurn(game, 1L, 3));
    }

    @Test
    void validateTurn_blackPlayerOnEvenMove_passes() {
        Game game = Game.builder()
                .whitePlayerId(1L)
                .blackPlayerId(2L)
                .build();

        // Black's turn (even move numbers)
        assertThatNoException().isThrownBy(() -> rules.validateTurn(game, 2L, 2));
        assertThatNoException().isThrownBy(() -> rules.validateTurn(game, 2L, 4));
    }

    @Test
    void validateTurn_blackPlayerOnOddMove_throws() {
        Game game = Game.builder()
                .whitePlayerId(1L)
                .blackPlayerId(2L)
                .build();

        // Black player trying to move on white's turn
        assertThatThrownBy(() -> rules.validateTurn(game, 2L, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not your turn");

        assertThatThrownBy(() -> rules.validateTurn(game, 2L, 3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not your turn");
    }

    @Test
    void validateTurn_whitePlayerOnEvenMove_throws() {
        Game game = Game.builder()
                .whitePlayerId(1L)
                .blackPlayerId(2L)
                .build();

        // White player trying to move on black's turn
        assertThatThrownBy(() -> rules.validateTurn(game, 1L, 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not your turn");

        assertThatThrownBy(() -> rules.validateTurn(game, 1L, 4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not your turn");
    }

    @Test
    void validateParticipant_whitePlayer_passes() {
        Game game = Game.builder()
                .whitePlayerId(1L)
                .blackPlayerId(2L)
                .build();

        assertThatNoException().isThrownBy(() -> rules.validateParticipant(game, 1L));
    }

    @Test
    void validateParticipant_blackPlayer_passes() {
        Game game = Game.builder()
                .whitePlayerId(1L)
                .blackPlayerId(2L)
                .build();

        assertThatNoException().isThrownBy(() -> rules.validateParticipant(game, 2L));
    }

    @Test
    void validateParticipant_nonParticipant_throws() {
        Game game = Game.builder()
                .whitePlayerId(1L)
                .blackPlayerId(2L)
                .build();

        assertThatThrownBy(() -> rules.validateParticipant(game, 3L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Not a participant");

        assertThatThrownBy(() -> rules.validateParticipant(game, 999L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Not a participant");
    }
}