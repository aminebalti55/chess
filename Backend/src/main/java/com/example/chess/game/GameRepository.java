package com.example.chess.game;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    @Query("select g from Game g where (g.whitePlayerId = :uid or g.blackPlayerId = :uid) " +
            "and g.status in (com.example.chess.game.GameStatus.CREATED, com.example.chess.game.GameStatus.STARTED) " +
            "order by g.updatedAt desc")
    List<Game> findActiveByUser(@Param("uid") Long userId);
}