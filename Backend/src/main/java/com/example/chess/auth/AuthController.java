package com.example.chess.auth;

import com.example.chess.common.Dto;
import com.example.chess.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/register")
    public ResponseEntity<Dto.AuthResponse> register(@RequestBody @Valid Dto.RegisterRequest req) {
        try {
            Dto.AuthResponse response = authService.register(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().equals("Email already in use")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            throw e;
        }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Dto.AuthResponse> login(@RequestBody @Valid Dto.LoginRequest req) {
        try {
            Dto.AuthResponse response = authService.login(req);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().equals("Invalid credentials")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            throw e;
        }
    }

    @GetMapping("/users/me")
    public Dto.CurrentUserDto me(@AuthenticationPrincipal User user) {
        return authService.toDto(user);
    }
}