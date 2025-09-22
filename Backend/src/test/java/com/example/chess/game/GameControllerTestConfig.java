package com.example.chess.game;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@TestConfiguration
public class GameControllerTestConfig {

    @Bean
    public GameService gameService() {
        return org.mockito.Mockito.mock(GameService.class);
    }

    @Bean
    public SimpMessagingTemplate messagingTemplate() {
        return org.mockito.Mockito.mock(SimpMessagingTemplate.class);
    }
}