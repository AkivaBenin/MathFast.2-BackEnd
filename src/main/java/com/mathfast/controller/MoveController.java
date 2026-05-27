package com.mathfast.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathfast.constant.RedisKeys;
import com.mathfast.dto.ApiResponse;
import com.mathfast.dto.MoveRequestDto;
import com.mathfast.entity.Participant;
import com.mathfast.entity.Room;
import com.mathfast.enums.GameState;
import com.mathfast.exception.ExploitDetectedException;
import com.mathfast.exception.WrongAnswerException;
import com.mathfast.repository.ParticipantRepository;
import com.mathfast.repository.RoomRepository;
import com.mathfast.service.RedisCacheService;
import com.mathfast.service.SseStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/race")
@RequiredArgsConstructor
public class MoveController {

    private final RedisCacheService redisCacheService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final SseStreamService sseStreamService;
    private final ObjectMapper objectMapper;

    @PostMapping("/{roomId:[0-9a-fA-F\\-]{36}}/move")
    public ResponseEntity<ApiResponse<Void>> makeMove(
            @PathVariable UUID roomId,
            @RequestBody MoveRequestDto moveRequest) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID playerId = UUID.fromString(authentication.getCredentials().toString());

        // 1. Safe parameter validation
        if (moveRequest.getNonce() == null || moveRequest.getNonce().trim().isEmpty()) {
            throw new ExploitDetectedException("Nonce cannot be blank.");
        }
        if (moveRequest.getAnswer() == null) {
            throw new ExploitDetectedException("Answer cannot be null.");
        }

        String difficulty = moveRequest.getDifficulty();
        if (difficulty == null) {
            difficulty = "REGULAR";
        }
        if (!difficulty.equals("HIGHWAY") && !difficulty.equals("DIRT_ROAD") && !difficulty.equals("REGULAR")) {
            throw new ExploitDetectedException("Invalid difficulty path route specified.");
        }

        // 2. Fetch expected solution from Redis
        String nonceKey = RedisKeys.getNonceKey(moveRequest.getNonce());
        String storedValue = stringRedisTemplate.opsForValue().get(nonceKey);
        
        if (storedValue == null || storedValue.trim().isEmpty()) {
            throw new ExploitDetectedException("Invalid or already used nonce detected.");
        }

        int expectedSolution;
        try {
            expectedSolution = Integer.parseInt(storedValue);
        } catch (NumberFormatException e) {
            throw new ExploitDetectedException("Invalid stored nonce metadata.");
        }

        // 3. Compare answers safely
        if (!Objects.equals(moveRequest.getAnswer(), expectedSolution)) {
            // Wrong answer: consume nonce immediately and throw WrongAnswerException
            stringRedisTemplate.delete(nonceKey);
            throw new WrongAnswerException("Incorrect answer.");
        }

        // 4. Award points on correct answer
        int points = switch (difficulty) {
            case "HIGHWAY" -> 30;
            case "DIRT_ROAD" -> 15;
            default -> 10;
        };

        // 5. Correct answer: Do NOT delete or write back in Java.
        // The Lua script validate_and_score.lua will check room state, check nonce exists,
        // delete the nonce atomically, and increment score.
        redisCacheService.processMove(
                roomId,
                playerId,
                moveRequest.getNonce(),
                points
        );

        // 6. Broadcast Real-Time updates & evaluate game finish transitions
        Room room = roomRepository.findById(roomId).orElse(null);
        int targetScore = room != null ? room.getTargetScore() : 100;

        List<Participant> participants = participantRepository.findByRoomId(roomId);
        Map<String, String> participantIdToNickname = new HashMap<>();
        for (Participant p : participants) {
            participantIdToNickname.put(p.getId().toString(), p.getNickname());
        }

        String scoresKey = RedisKeys.getRoomScoresKey(roomId);
        Map<Object, Object> scores = stringRedisTemplate.opsForHash().entries(scoresKey);

        List<Map<String, Object>> leaderboard = new ArrayList<>();
        boolean targetReached = false;

        for (Map.Entry<Object, Object> entry : scores.entrySet()) {
            String pId = entry.getKey().toString();
            int score = Integer.parseInt(entry.getValue().toString());
            String pNickname = participantIdToNickname.getOrDefault(pId, "Unknown");

            Map<String, Object> playerMap = new HashMap<>();
            playerMap.put("id", pId);
            playerMap.put("nickname", pNickname);
            playerMap.put("score", score);
            leaderboard.add(playerMap);

            if (score >= targetScore) {
                targetReached = true;
            }
        }

        // Sort by score descending
        leaderboard.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));

        // Broadcast current live scores
        sseStreamService.sendToRoom(roomId, "SCORE_UPDATE", Map.of(
                "roomId", roomId.toString(),
                "leaderboard", leaderboard
        ));
        sseStreamService.sendToRoom(roomId, "LEADERBOARD_UPDATE", Map.of(
                "roomId", roomId.toString(),
                "leaderboard", leaderboard
        ));

        // 7. Transition Room State if a player reached the target score
        String currentRoomState = stringRedisTemplate.opsForValue().get(RedisKeys.getRoomStateKey(roomId));
        if (targetReached && !"FINISHED".equals(currentRoomState)) {
            updateRoomState(roomId, "FINISHED");

            try {
                stringRedisTemplate.opsForValue().set(
                        RedisKeys.getRoomResultsKey(roomId),
                        objectMapper.writeValueAsString(leaderboard)
                );
            } catch (Exception e) {
                log.error("Failed to serialize final leaderboard results to Redis", e);
            }

            // Broadcast GAME_OVER and STATE_CHANGE events
            sseStreamService.sendToRoom(roomId, "GAME_OVER", Map.of(
                    "state", "FINISHED",
                    "roomId", roomId.toString(),
                    "results", leaderboard
            ));
            sseStreamService.sendToRoom(roomId, "STATE_CHANGE", Map.of(
                    "state", "FINISHED",
                    "roomId", roomId.toString()
            ));
        }

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private void updateRoomState(UUID roomId, String newState) {
        stringRedisTemplate.opsForValue().set(RedisKeys.getRoomStateKey(roomId), newState);
        String stateJson = stringRedisTemplate.opsForValue().get(RedisKeys.getRoomStateJson(roomId));
        if (stateJson != null) {
            try {
                Map<String, Object> stateMap = objectMapper.readValue(stateJson, Map.class);
                stateMap.put("state", newState);
                stringRedisTemplate.opsForValue().set(RedisKeys.getRoomStateJson(roomId), objectMapper.writeValueAsString(stateMap));
            } catch(Exception e) {}
        }
        
        roomRepository.findById(roomId).ifPresent(room -> {
            room.setStatus(GameState.valueOf(newState));
            roomRepository.save(room);
        });
    }
}
