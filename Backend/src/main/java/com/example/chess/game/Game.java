package com.example.chess.game;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(
        name = "games",
        indexes = {
                @Index(name = "idx_games_updated_at", columnList = "updated_at"),
                @Index(name = "idx_games_white_player", columnList = "white_player_id"),
                @Index(name = "idx_games_black_player", columnList = "black_player_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "white_player_id", nullable = false)
    private Long whitePlayerId;

    @Column(name = "black_player_id", nullable = false)
    private Long blackPlayerId;

    @Enumerated(EnumType.STRING)                      // Java/JSON shows CREATED/STARTED/FINISHED
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)               // Bind as Postgres named enum, not VARCHAR
    @Column(name = "status", columnDefinition = "game_status", nullable = false)
    private GameStatus status;

    @Column(name = "last_fen", columnDefinition = "text")
    private String lastFen; // optional snapshot

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = GameStatus.STARTED; // default when created via accept
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
