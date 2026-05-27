package com.mathfast.controller;

import com.mathfast.entity.GameHistory;
import com.mathfast.entity.Room;
import com.mathfast.repository.GameHistoryRepository;
import com.mathfast.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final RoomRepository roomRepository;
    private final GameHistoryRepository gameHistoryRepository;

    @Deprecated
    @GetMapping("/results/{roomId}")
    public ResponseEntity<List<GameHistory>> getPodiumResults(@PathVariable UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        List<GameHistory> results = gameHistoryRepository.findTop3PodiumFinishers(room.getRoomCode());
        return ResponseEntity.ok(results);
    }
}
