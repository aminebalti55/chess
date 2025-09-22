package com.example.chess.game;

import com.example.chess.common.Dto;
import com.example.chess.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.security.Principal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GameController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GameControllerTest.MockConfig.class)
class GameControllerTest {

    @TestConfiguration
    static class MockConfig implements WebMvcConfigurer {
        @Bean
        GameService gameService() {
            return mock(GameService.class);
        }

        @Bean
        SimpMessagingTemplate messagingTemplate() {
            return mock(SimpMessagingTemplate.class);
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                            && parameter.getParameterType().equals(User.class);
                }

                @Override
                public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                              NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                    Principal principal = webRequest.getUserPrincipal();
                    if (principal instanceof Authentication) {
                        Authentication auth = (Authentication) principal;
                        return auth.getPrincipal();
                    }
                    return null;
                }
            });
        }
    }

    @Autowired private MockMvc mvc;
    @Autowired private GameService gameService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test to avoid cross-test interference
        reset(gameService, messagingTemplate);
    }

    @Test
    void active_returnsActiveGamesForUser() throws Exception {
        var user = User.builder().id(1L).displayName("Alice").email("alice@test.com").build();

        List<Game> activeGames = List.of(
                Game.builder()
                        .id(10L).whitePlayerId(1L).blackPlayerId(2L)
                        .status(GameStatus.STARTED)
                        .lastFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")
                        .build(),
                Game.builder()
                        .id(20L).whitePlayerId(3L).blackPlayerId(1L)
                        .status(GameStatus.STARTED)
                        .lastFen(null)
                        .build()
        );

        when(gameService.getActiveGamesFor(1L)).thenReturn(activeGames);

        mvc.perform(get("/api/games/active")
                        .principal(new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].gameId").value(10))
                .andExpect(jsonPath("$[0].youAreWhite").value(true))
                .andExpect(jsonPath("$[0].status").value("STARTED"))
                .andExpect(jsonPath("$[0].lastFen").value("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"))
                .andExpect(jsonPath("$[1].gameId").value(20))
                .andExpect(jsonPath("$[1].youAreWhite").value(false))
                .andExpect(jsonPath("$[1].status").value("STARTED"))
                .andExpect(jsonPath("$[1].lastFen").isEmpty());

        verify(gameService).getActiveGamesFor(1L);
    }

    @Test
    void active_emptyList_returnsEmptyArray() throws Exception {
        var user = User.builder().id(1L).displayName("Alice").email("alice@test.com").build();
        when(gameService.getActiveGamesFor(1L)).thenReturn(List.of());

        mvc.perform(get("/api/games/active")
                        .principal(new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(gameService).getActiveGamesFor(1L);
    }

    @Test
    void moves_returnsOrderedMoveList() throws Exception {
        var user = User.builder().id(1L).displayName("Alice").email("alice@test.com").build();
        Long gameId = 42L;

        List<Move> moves = List.of(
                Move.builder()
                        .moveNumber(1).fromSquare("e2").toSquare("e4").san("e4")
                        .promotion(null).playedByUserId(1L)
                        .playedAt(Instant.parse("2024-01-01T10:00:00Z"))
                        .fenAfter("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")
                        .build(),
                Move.builder()
                        .moveNumber(2).fromSquare("e7").toSquare("e5").san("e5")
                        .promotion(null).playedByUserId(2L)
                        .playedAt(Instant.parse("2024-01-01T10:01:00Z"))
                        .fenAfter("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2")
                        .build()
        );

        when(gameService.listMoves(gameId)).thenReturn(moves);

        mvc.perform(get("/api/games/{id}/moves", gameId)
                        .principal(new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].moveNumber").value(1))
                .andExpect(jsonPath("$[0].fromSquare").value("e2"))
                .andExpect(jsonPath("$[0].toSquare").value("e4"))
                .andExpect(jsonPath("$[0].san").value("e4"))
                .andExpect(jsonPath("$[0].promotion").isEmpty())
                .andExpect(jsonPath("$[0].playedByUserId").value(1))
                .andExpect(jsonPath("$[0].fenAfter").value("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"))
                .andExpect(jsonPath("$[1].moveNumber").value(2))
                .andExpect(jsonPath("$[1].fromSquare").value("e7"))
                .andExpect(jsonPath("$[1].toSquare").value("e5"))
                .andExpect(jsonPath("$[1].san").value("e5"))
                .andExpect(jsonPath("$[1].playedByUserId").value(2));

        verify(gameService).listMoves(gameId);
    }

    @Test
    void moves_emptyGame_returnsEmptyArray() throws Exception {
        var user = User.builder().id(1L).displayName("Alice").email("alice@test.com").build();
        Long gameId = 42L;

        when(gameService.listMoves(gameId)).thenReturn(List.of());

        mvc.perform(get("/api/games/{id}/moves", gameId)
                        .principal(new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(gameService).listMoves(gameId);
    }

    @Test
    void submitMove_validMove_recordsAndBroadcasts() {
        var user = User.builder().id(1L).displayName("Alice").email("alice@test.com").build();
        Principal principal = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

        Long gameId = 42L;
        Dto.MoveSend request = new Dto.MoveSend("e2", "e4", null, "e4");

        Move recordedMove = Move.builder()
                .moveNumber(1).fromSquare("e2").toSquare("e4").san("e4")
                .promotion(null).playedByUserId(1L)
                .playedAt(Instant.parse("2024-01-01T10:00:00Z"))
                .build();

        when(gameService.recordMove(gameId, 1L, request)).thenReturn(recordedMove);

        GameController controller = new GameController(gameService, messagingTemplate);

        controller.submitMove(gameId, request, principal);

        verify(gameService).recordMove(gameId, 1L, request);

        // Capture payload to avoid convertAndSend overload ambiguity
        ArgumentCaptor<Dto.MoveBroadcast> payload = ArgumentCaptor.forClass(Dto.MoveBroadcast.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/games/42"), payload.capture());

        var broadcast = payload.getValue();
        assertThat(broadcast.moveNumber()).isEqualTo(1);
        assertThat(broadcast.from()).isEqualTo("e2");
        assertThat(broadcast.to()).isEqualTo("e4");
        assertThat(broadcast.san()).isEqualTo("e4");
        assertThat(broadcast.by()).isEqualTo(1L);
        assertThat(broadcast.promotion()).isNull();
    }

    @Test
    void submitMove_withPromotion_recordsAndBroadcastsCorrectly() {
        var user = User.builder().id(2L).displayName("Bob").email("bob@test.com").build();
        Principal principal = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

        Long gameId = 55L;
        Dto.MoveSend request = new Dto.MoveSend("e7", "e8", "q", "e8=Q");

        Move recordedMove = Move.builder()
                .moveNumber(15).fromSquare("e7").toSquare("e8").san("e8=Q")
                .promotion("q").playedByUserId(2L)
                .playedAt(Instant.now())
                .build();

        when(gameService.recordMove(gameId, 2L, request)).thenReturn(recordedMove);

        GameController controller = new GameController(gameService, messagingTemplate);

        controller.submitMove(gameId, request, principal);

        verify(gameService).recordMove(gameId, 2L, request);

        ArgumentCaptor<Dto.MoveBroadcast> payload = ArgumentCaptor.forClass(Dto.MoveBroadcast.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/games/55"), payload.capture());

        var broadcast = payload.getValue();
        assertThat(broadcast.moveNumber()).isEqualTo(15);
        assertThat(broadcast.from()).isEqualTo("e7");
        assertThat(broadcast.to()).isEqualTo("e8");
        assertThat(broadcast.san()).isEqualTo("e8=Q");
        assertThat(broadcast.by()).isEqualTo(2L);
        assertThat(broadcast.promotion()).isEqualTo("q");
    }

    @Test
    void submitMove_serviceThrows_doesNotBroadcast() {
        var user = User.builder().id(1L).displayName("Alice").email("alice@test.com").build();
        Principal principal = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

        Long gameId = 42L;
        Dto.MoveSend request = new Dto.MoveSend("e2", "e4", null, "e4");

        when(gameService.recordMove(gameId, 1L, request))
                .thenThrow(new IllegalStateException("Not your turn"));

        GameController controller = new GameController(gameService, messagingTemplate);

        assertThatThrownBy(() -> controller.submitMove(gameId, request, principal))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not your turn");

        verify(gameService).recordMove(gameId, 1L, request);
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}