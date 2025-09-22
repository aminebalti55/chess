package com.example.chess.auth;

import com.example.chess.common.Dto;
import com.example.chess.user.User;
import com.example.chess.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private PasswordEncoder encoder;

    private JwtService jwt;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        jwt = new JwtService("01234567890123456789012345678901");
        auth = new AuthService(users, encoder, jwt);
    }

    @Test
    void register_success() {
        // Given
        var req = new Dto.RegisterRequest("New@Email.com", "Alice", "Passw0rd!");
        when(users.existsByEmail("new@email.com")).thenReturn(false);
        when(encoder.encode("Passw0rd!")).thenReturn("$bcrypt$hashedPassword");
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // When
        Dto.AuthResponse resp = auth.register(req);

        // Then
        assertThat(resp.token()).isNotBlank();
        assertThat(resp.userId()).isEqualTo(1L);
        assertThat(resp.email()).isEqualTo("new@email.com");
        assertThat(resp.displayName()).isEqualTo("Alice");

        verify(users).existsByEmail("new@email.com");
        verify(encoder).encode("Passw0rd!");
        verify(users).save(argThat(user ->
                user.getEmail().equals("new@email.com") &&
                        user.getDisplayName().equals("Alice") &&
                        user.getPasswordHash().equals("$bcrypt$hashedPassword")
        ));
    }

    @Test
    void register_conflict() {
        // Given
        var req = new Dto.RegisterRequest("existing@example.com", "Alice", "x1234567");
        when(users.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> auth.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use");

        verify(users).existsByEmail("existing@example.com");
        verify(users, never()).save(any());
    }

    @Test
    void register_normalizesEmailAndDisplayName() {
        // Given
        var req = new Dto.RegisterRequest("  UPPER@EXAMPLE.COM  ", "  Alice  ", "Passw0rd!");
        when(users.existsByEmail("upper@example.com")).thenReturn(false);
        when(encoder.encode("Passw0rd!")).thenReturn("$hashed");
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        // When
        Dto.AuthResponse resp = auth.register(req);

        // Then
        assertThat(resp.email()).isEqualTo("upper@example.com");
        assertThat(resp.displayName()).isEqualTo("Alice");
    }

    @Test
    void login_success() {
        // Given
        var req = new Dto.LoginRequest("alice@example.com", "s3cret");
        User user = User.builder()
                .id(9L)
                .email("alice@example.com")
                .displayName("Alice")
                .passwordHash("$hashedPassword")
                .build();

        when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("s3cret", "$hashedPassword")).thenReturn(true);

        // When
        var resp = auth.login(req);

        // Then
        assertThat(resp.userId()).isEqualTo(9L);
        assertThat(resp.email()).isEqualTo("alice@example.com");
        assertThat(resp.displayName()).isEqualTo("Alice");
        assertThat(resp.token()).isNotBlank();

        verify(users).findByEmail("alice@example.com");
        verify(encoder).matches("s3cret", "$hashedPassword");
    }

    @Test
    void login_badCreds_emailMissing() {
        // Given
        var req = new Dto.LoginRequest("nonexistent@example.com", "password");
        when(users.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> auth.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");

        verify(users).findByEmail("nonexistent@example.com");
        verify(encoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_badCreds_passwordMismatch() {
        // Given
        var req = new Dto.LoginRequest("alice@example.com", "wrongpassword");
        User user = User.builder()
                .id(9L)
                .email("alice@example.com")
                .displayName("Alice")
                .passwordHash("$correctHash")
                .build();

        when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("wrongpassword", "$correctHash")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> auth.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");

        verify(users).findByEmail("alice@example.com");
        verify(encoder).matches("wrongpassword", "$correctHash");
    }

    @Test
    void login_normalizesEmail() {
        // Given
        var req = new Dto.LoginRequest("  ALICE@EXAMPLE.COM  ", "password");
        User user = User.builder()
                .id(10L)
                .email("alice@example.com")
                .displayName("Alice")
                .passwordHash("$hash")
                .build();

        when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("password", "$hash")).thenReturn(true);

        // When
        var resp = auth.login(req);

        // Then
        assertThat(resp.userId()).isEqualTo(10L);
        verify(users).findByEmail("alice@example.com");
    }

    @Test
    void toDto_success() {
        // Given
        User user = User.builder()
                .id(42L)
                .email("test@example.com")
                .displayName("Test User")
                .build();

        // When
        Dto.CurrentUserDto dto = auth.toDto(user);

        // Then
        assertThat(dto.id()).isEqualTo(42L);
        assertThat(dto.email()).isEqualTo("test@example.com");
        assertThat(dto.displayName()).isEqualTo("Test User");
    }
}