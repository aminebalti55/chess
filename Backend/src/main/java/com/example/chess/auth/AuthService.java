package com.example.chess.auth;

import com.example.chess.common.Dto;
import com.example.chess.user.User;
import com.example.chess.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;

    @Transactional
    public Dto.AuthResponse register(Dto.RegisterRequest req) {
        // Normalize email and displayName
        String normalizedEmail = req.email().toLowerCase().trim();
        String normalizedDisplayName = req.displayName().trim();

        // Check if email already exists
        if (users.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already in use");
        }

        // Hash password
        String passwordHash = passwordEncoder.encode(req.password());

        // Build and save user
        User user = User.builder()
                .email(normalizedEmail)
                .displayName(normalizedDisplayName)
                .passwordHash(passwordHash)
                .createdAt(Instant.now())
                .build();

        user = users.save(user);

        // Issue token
        String token = jwt.generateToken(user, Duration.ofDays(7));

        return new Dto.AuthResponse(token, user.getId(), user.getEmail(), user.getDisplayName());
    }

    @Transactional(readOnly = true)
    public Dto.AuthResponse login(Dto.LoginRequest req) {
        // Normalize email
        String normalizedEmail = req.email().toLowerCase().trim();

        // Lookup user by email
        User user = users.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        // Verify password
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Issue token
        String token = jwt.generateToken(user, Duration.ofDays(7));

        return new Dto.AuthResponse(token, user.getId(), user.getEmail(), user.getDisplayName());
    }

    @Transactional(readOnly = true)
    public Dto.CurrentUserDto toDto(User user) {
        return new Dto.CurrentUserDto(user.getId(), user.getEmail(), user.getDisplayName());
    }
}