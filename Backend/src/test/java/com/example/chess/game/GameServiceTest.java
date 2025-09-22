package com.example.chess.game;

import com.example.chess.common.Dto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private MoveRepository moveRepository;

    @Mock
    private GameRules gameRules;

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, moveRepository, gameRules);
    }

    @Test
    void createGame_success_savesWithCorrectPlayersAndStatus() {
        // Given
        Long whitePlayerId = 1L;
        Long blackPlayerId = 2L;

        Game savedGame = Game.builder()
                .id(100L)
                .whitePlayerId(whitePlayerId)
                .blackPlayerId(blackPlayerId)
                .status(GameStatus.STARTED)
                .build();

        when(gameRepository.save(any(Game.class))).thenReturn(savedGame);

        // When
        Game result = gameService.createGame(whitePlayerId, blackPlayerId);

        // Then
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getWhitePlayerId()).isEqualTo(whitePlayerId);
        assertThat(result.getBlackPlayerId()).isEqualTo(blackPlayerId);
        assertThat(result.getStatus()).isEqualTo(GameStatus.STARTED);

        verify(gameRepository).save(argThat(game ->
                game.getWhitePlayerId().equals(whitePlayerId) &&
                        game.getBlackPlayerId().equals(blackPlayerId) &&
                        game.getStatus() == GameStatus.STARTED &&
                        game.getCreatedAt() != null &&
                        game.getUpdatedAt() != null
        ));
    }

    @Test
    void listMoves_success_returnsOrderedMoves() {
        // Given
        Long gameId = 1L;
        List<Move> moves = List.of(
                Move.builder().moveNumber(1).fromSquare("e2").toSquare("e4").build(),
                Move.builder().moveNumber(2).fromSquare("e7").toSquare("e5").build()
        );

        when(moveRepository.findByGameIdOrderByMoveNumberAsc(gameId)).thenReturn(moves);

        // When
        List<Move> result = gameService.listMoves(gameId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMoveNumber()).isEqualTo(1);
        assertThat(result.get(1).getMoveNumber()).isEqualTo(2);
        verify(moveRepository).findByGameIdOrderByMoveNumberAsc(gameId);
    }

    @Test
    void getActiveGamesFor_success_returnsActiveGames() {
        // Given
        Long userId = 1L;
        List<Game> games = List.of(
                Game.builder().id(1L).whitePlayerId(userId).status(GameStatus.STARTED).build(),
                Game.builder().id(2L).blackPlayerId(userId).status(GameStatus.STARTED).build()
        );

        when(gameRepository.findActiveByUser(userId)).thenReturn(games);

        // When
        List<Game> result = gameService.getActiveGamesFor(userId);

        // Then
        assertThat(result).hasSize(2);
        verify(gameRepository).findActiveByUser(userId);
    }

    @Test
    void recordMove_success_validatesAndSavesMove() {
        // Given
        Long gameId = 1L;
        Long userId = 10L;
        Dto.MoveSend request = new Dto.MoveSend("e2", "e4", null, "e4");

        Instant oldUpdatedAt = Instant.now().minusSeconds(60);
        Game game = Game.builder()
                .id(gameId)
                .whitePlayerId(userId)
                .blackPlayerId(20L)
                .status(GameStatus.STARTED)
                .updatedAt(oldUpdatedAt)
                .build();

        Move savedMove = Move.builder()
                .id(100L)
                .gameId(gameId)
                .moveNumber(1)
                .fromSquare("e2")
                .toSquare("e4")
                .san("e4")
                .playedByUserId(userId)
                .playedAt(Instant.now())
                .build();

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(moveRepository.lastMoveNumber(gameId)).thenReturn(0);
        when(moveRepository.save(any(Move.class))).thenReturn(savedMove);
        when(gameRepository.save(any(Game.class))).thenReturn(game);

        // When
        Move result = gameService.recordMove(gameId, userId, request);

        // Then
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getMoveNumber()).isEqualTo(1);
        assertThat(result.getFromSquare()).isEqualTo("e2");
        assertThat(result.getToSquare()).isEqualTo("e4");
        assertThat(result.getSan()).isEqualTo("e4");
        assertThat(result.getPlayedByUserId()).isEqualTo(userId);

        // Verify all validations were called
        verify(gameRules).validateParticipant(game, userId);
        verify(gameRules).validateSquares("e2", "e4");
        verify(gameRules).validateTurn(game, userId, 1);

        // Verify move was saved
        verify(moveRepository).save(argThat(move ->
                move.getGameId().equals(gameId) &&
                        move.getMoveNumber().equals(1) &&
                        move.getFromSquare().equals("e2") &&
                        move.getToSquare().equals("e4") &&
                        move.getSan().equals("e4") &&
                        move.getPlayedByUserId().equals(userId)
        ));

        // Verify game updatedAt was touched - capture the old time before mutation
        verify(gameRepository).save(argThat(g ->
                g.getId().equals(gameId) &&
                        g.getUpdatedAt().isAfter(oldUpdatedAt)
        ));
    }

    @Test
    void recordMove_gameNotFound_throws() {
        // Given
        Long gameId = 999L;
        Long userId = 1L;
        Dto.MoveSend request = new Dto.MoveSend("e2", "e4", null, null);

        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> gameService.recordMove(gameId, userId, request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Game not found");

        verify(gameRules, never()).validateParticipant(any(), any());
        verify(gameRules, never()).validateSquares(any(), any());
        verify(gameRules, never()).validateTurn(any(), any(), anyInt());
        verify(moveRepository, never()).save(any());
    }

    @Test
    void recordMove_notParticipant_throws() {
        // Given
        Long gameId = 1L;
        Long userId = 999L; // not a participant
        Dto.MoveSend request = new Dto.MoveSend("e2", "e4", null, null);

        Game game = Game.builder()
                .id(gameId)
                .whitePlayerId(1L)
                .blackPlayerId(2L)
                .status(GameStatus.STARTED)
                .build();

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        doThrow(new AccessDeniedException("Not a participant"))
                .when(gameRules).validateParticipant(game, userId);

        // When & Then
        assertThatThrownBy(() -> gameService.recordMove(gameId, userId, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Not a participant");

        verify(gameRules).validateParticipant(game, userId);
        verify(gameRules, never()).validateSquares(any(), any());
        verify(gameRules, never()).validateTurn(any(), any(), anyInt());
        verify(moveRepository, never()).save(any());
    }

    @Test
    void recordMove_invalidSquares_throws() {
        // Given
        Long gameId = 1L;
        Long userId = 1L;
        Dto.MoveSend request = new Dto.MoveSend("z9", "e4", null, null);

        Game game = Game.builder()
                .id(gameId)
                .whitePlayerId(userId)
                .blackPlayerId(2L)
                .status(GameStatus.STARTED)
                .build();

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        doThrow(new IllegalArgumentException("Invalid squares"))
                .when(gameRules).validateSquares("z9", "e4");

        // When & Then
        assertThatThrownBy(() -> gameService.recordMove(gameId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid squares");

        verify(gameRules).validateParticipant(game, userId);
        verify(gameRules).validateSquares("z9", "e4");
        verify(gameRules, never()).validateTurn(any(), any(), anyInt());
        verify(moveRepository, never()).save(any());
    }

    @Test
    void recordMove_wrongTurn_throws() {
        // Given
        Long gameId = 1L;
        Long userId = 2L; // black player
        Dto.MoveSend request = new Dto.MoveSend("e2", "e4", null, null);

        Game game = Game.builder()
                .id(gameId)
                .whitePlayerId(1L)
                .blackPlayerId(userId)
                .status(GameStatus.STARTED)
                .build();

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(moveRepository.lastMoveNumber(gameId)).thenReturn(0); // move 1 = white's turn
        doThrow(new IllegalStateException("Not your turn"))
                .when(gameRules).validateTurn(game, userId, 1);

        // When & Then
        assertThatThrownBy(() -> gameService.recordMove(gameId, userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not your turn");

        verify(gameRules).validateParticipant(game, userId);
        verify(gameRules).validateSquares("e2", "e4");
        verify(gameRules).validateTurn(game, userId, 1);
        verify(moveRepository, never()).save(any());
    }

    @Test
    void recordMove_withPromotion_savesCorrectly() {
        // Given
        Long gameId = 1L;
        Long userId = 1L;
        Dto.MoveSend request = new Dto.MoveSend("e7", "e8", "q", "e8=Q");

        Game game = Game.builder()
                .id(gameId)
                .whitePlayerId(userId)
                .blackPlayerId(2L)
                .status(GameStatus.STARTED)
                .build();

        Move savedMove = Move.builder()
                .id(101L)
                .gameId(gameId)
                .moveNumber(5)
                .fromSquare("e7")
                .toSquare("e8")
                .promotion("q")
                .san("e8=Q")
                .playedByUserId(userId)
                .build();

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(moveRepository.lastMoveNumber(gameId)).thenReturn(4); // move 5 = white's turn
        when(moveRepository.save(any(Move.class))).thenReturn(savedMove);
        when(gameRepository.save(any(Game.class))).thenReturn(game);

        // When
        Move result = gameService.recordMove(gameId, userId, request);

        // Then
        assertThat(result.getPromotion()).isEqualTo("q");
        assertThat(result.getSan()).isEqualTo("e8=Q");

        verify(moveRepository).save(argThat(move ->
                move.getPromotion().equals("q") &&
                        move.getSan().equals("e8=Q")
        ));
    }

    @Test
    void createGameDto_returnsCorrectDto() {
        // Given
        Long player1Id = 1L;
        Long player2Id = 2L;

        Game savedGame = Game.builder()
                .id(42L)
                .whitePlayerId(player1Id)
                .blackPlayerId(player2Id)
                .status(GameStatus.STARTED)
                .build();

        when(gameRepository.save(any(Game.class))).thenReturn(savedGame);

        // When
        Dto.GameCreated result = gameService.createGameDto(player1Id, player2Id);

        // Then
        assertThat(result.gameId()).isEqualTo("42");
        assertThat(result.whitePlayerId()).isEqualTo(player1Id);
        assertThat(result.blackPlayerId()).isEqualTo(player2Id);
    }
}