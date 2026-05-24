package com.mathfast.repository;

import com.mathfast.entity.GameHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistory, UUID> {

    @Query(value = "SELECT * FROM game_history WHERE room_code = :roomCode ORDER BY score DESC LIMIT 3", nativeQuery = true)
    List<GameHistory> findTop3PodiumFinishers(@Param("roomCode") String roomCode);
}
