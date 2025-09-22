package com.example.chess.game;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "moves", uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "move_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Move {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private Integer moveNumber;

    @Column(nullable = false, length = 2)
    private String fromSquare; // e.g., "e2"

    @Column(nullable = false, length = 2)
    private String toSquare; // e.g., "e4"

    @Column(length = 16)
    private String san; // optional algebraic: "e4", "Nf3"

    @Column(length = 1)
    private String promotion; // q/r/b/n if present

    @Column(columnDefinition = "text")
    private String fenAfter; // optional snapshot for fast resume

    @Column(nullable = false)
    private Long playedByUserId;

    @Column(nullable = false)
    private Instant playedAt;
}