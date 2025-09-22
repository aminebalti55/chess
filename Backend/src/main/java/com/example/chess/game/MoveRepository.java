package com.example.chess.game;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {

    List<Move> findByGameIdOrderByMoveNumberAsc(Long gameId);

    @Query("select coalesce(max(m.moveNumber), 0) from Move m where m.gameId = :gid")
    int lastMoveNumber(@Param("gid") Long gameId);
}