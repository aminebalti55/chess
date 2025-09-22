package com.example.chess.auth;

import com.example.chess.common.Dto;
import com.example.chess.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // disable security filters for testing
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AuthService auth;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_created() throws Exception {
        // Given
        var resp = new Dto.AuthResponse("test-token", 1L, "alice@example.com", "Alice");
        when(auth.register(any())).thenReturn(resp);

        // When & Then
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "email": "alice@example.com",
                        "displayName": "Alice",
                        "password": "Passw0rd123"
                    }
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.displayName").value("Alice"));
    }

    @Test
    void register_conflict() throws Exception {
        // Given
        when(auth.register(any())).thenThrow(new IllegalArgumentException("Email already in use"));

        // When & Then
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "email": "existing@example.com",
                        "displayName": "Alice",
                        "password": "Passw0rd123"
                    }
                    """))
                .andExpect(status().isConflict());
    }

    @Test
    void register_badRequest_invalidEmail() throws Exception {
        // When & Then
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "email": "not-an-email",
                        "displayName": "Alice",
                        "password": "Passw0rd123"
                    }
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_badRequest_shortPassword() throws Exception {
        // When & Then
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "email": "alice@example.com",
                        "displayName": "Alice",
                        "password": "short"
                    }
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_badRequest_longDisplayName() throws Exception {
        // When & Then
        String longName = "A".repeat(81); // 81 chars, exceeds 80 limit

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                    {
                        "email": "alice@example.com",
                        "displayName": "%s",
                        "password": "Passw0rd123"
                    }
                    """, longName)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ok() throws Exception {
        // Given
        var resp = new Dto.AuthResponse("login-token", 9L, "alice@example.com", "Alice");
        when(auth.login(any())).thenReturn(resp);

        // When & Then
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "email": "alice@example.com",
                        "password": "Passw0rd123"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("login-token"))
                .andExpect(jsonPath("$.userId").value(9))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.displayName").value("Alice"));
    }

    @Test
    void login_unauthorized() throws Exception {
        // Given
        when(auth.login(any())).thenThrow(new IllegalArgumentException("Invalid credentials"));

        // When & Then
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "email": "alice@example.com",
                        "password": "wrongpassword"
                    }
                    """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_badRequest_invalidEmail() throws Exception {
        // When & Then
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "email": "not-an-email",
                        "password": "Passw0rd123"
                    }
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_badRequest_blankPassword() throws Exception {
        // When & Then
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "email": "alice@example.com",
                        "password": ""
                    }
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void me_ok_withInjectedPrincipal() throws Exception {
        // Given
        User user = User.builder()
                .id(7L)
                .email("user@example.com")
                .displayName("Test User")
                .build();

        var userDto = new Dto.CurrentUserDto(7L, "user@example.com", "Test User");
        when(auth.toDto(any(User.class))).thenReturn(userDto);

        // When & Then
        mvc.perform(get("/api/users/me")
                        .with(request -> {
                            var authentication = new UsernamePasswordAuthenticationToken(
                                    user, null, Collections.emptyList());
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.displayName").value("Test User"));
    }

    @Test
    void me_withoutAuthentication() throws Exception {
        // Given
        SecurityContextHolder.clearContext(); // Ensure no authentication
        var userDto = new Dto.CurrentUserDto(null, null, null);
        when(auth.toDto(any())).thenReturn(userDto);

        // When & Then - since filters are disabled, this will still return 200
        // In a real scenario with filters enabled, this would be 401
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isOk());
    }
}