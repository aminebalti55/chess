package com.example.chess.common;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public class Dto {

  // Authentication DTOs
  public record RegisterRequest(
          @NotBlank @Email String email,
          @NotBlank @Size(min = 2, max = 80) String displayName,
          @NotBlank @Size(min = 8, max = 72) String password
  ) {}

  public record LoginRequest(
          @NotBlank @Email String email,
          @NotBlank String password
  ) {}

  public record AuthResponse(
          String token,
          Long userId,
          String email,
          String displayName
  ) {}

  public record CurrentUserDto(
          Long id,
          String email,
          String displayName
  ) {}

  // WebSocket/Lobby DTOs
  public record UserLite(
          Long id,
          String displayName
  ) {}

  public record OnlineUsersDto(
          List<UserLite> users
  ) {}

  public record InviteSend(
          @NotNull Long toUserId
  ) {}

  public record InviteReply(
          @NotNull Long invitationId,
          @NotNull Boolean accept
  ) {}

  public record InviteNotification(
          Long invitationId,
          UserLite fromUser
  ) {}

  // Added from paste 1 - was missing
  public record InviteDeclined(
          String invitationId,
          UserLite byUser
  ) {}

  public record GameCreated(
          String gameId,
          Long whitePlayerId,
          Long blackPlayerId
  ) {}

  // Game/Move DTOs
  public record MoveSend(
          @NotBlank @Pattern(regexp = "^[a-h][1-8]$") String from,
          @NotBlank @Pattern(regexp = "^[a-h][1-8]$") String to,
          @Pattern(regexp = "^[qrbn]$") String promotion,
          String san
  ) {}

  public record MoveBroadcast(
          Integer moveNumber,
          String from,
          String to,
          String san,
          Long by,
          Instant ts,
          String promotion
  ) {}

  public record MoveRecord(
          Integer moveNumber,
          String fromSquare,
          String toSquare,
          String san,
          String promotion,
          Long playedByUserId,
          Instant playedAt,
          String fenAfter
  ) {}

  public record ActiveGameDto(
          Long gameId,
          Boolean youAreWhite,
          String status,
          String lastFen
  ) {}
}