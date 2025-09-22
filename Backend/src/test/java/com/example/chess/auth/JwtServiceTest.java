package com.example.chess.auth;

import com.example.chess.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwt;

    @BeforeEach
    void setUp() {
        jwt = new JwtService("01234567890123456789012345678901"); // 32 chars minimum
    }

    @Test
    void generatesAndParsesToken() {
        // Given
        User user = User.builder()
                .id(42L)
                .email("alice@example.com")
                .displayName("Alice")
                .build();

        // When
        String token = jwt.generateToken(user, Duration.ofMinutes(15));
        Long userId = jwt.getUserId(token);

        // Then
        assertThat(token).isNotBlank();
        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void generatesTokenWithCorrectClaims() {
        // Given
        User user = User.builder()
                .id(123L)
                .email("test@example.com")
                .displayName("Test User")
                .build();

        // When
        String token = jwt.generateToken(user, Duration.ofHours(1));

        // Then
        assertThat(token).isNotBlank();
        assertThat(jwt.getUserId(token)).isEqualTo(123L);
    }

    @Test
    void throwsExceptionForInvalidToken() {
        // When & Then
        assertThatThrownBy(() -> jwt.getUserId("invalid.token.here"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void throwsExceptionForShortSecret() {
        // When & Then
        assertThatThrownBy(() -> new JwtService("short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT secret must be at least 32 characters");
    }
}