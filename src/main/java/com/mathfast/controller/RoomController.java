package com.mathfast.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathfast.constant.RedisKeys;
import com.mathfast.dto.RoomDto;
import com.mathfast.entity.Participant;
import com.mathfast.entity.Room;
import com.mathfast.enums.GameState;
import com.mathfast.mapper.EntityMapper;
import com.mathfast.repository.ParticipantRepository;
import com.mathfast.repository.RoomRepository;
import com.mathfast.service.SseStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ScheduledExecutorService scheduledExecutorService;
    private final SseStreamService sseStreamService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createRoom(@RequestParam(required = false) UUID oldRoomId) {
        Room room = new Room();
        room.setRoomCode(generateUniqueRoomCode());
        room.setStatus(GameState.LOBBY);
        room.setTargetScore(100);
        room.setTargetQuestions(10);
        room = roomRepository.save(room);
        
        stringRedisTemplate.opsForValue().set(RedisKeys.getRoomStateKey(room.getId()), "LOBBY");
        stringRedisTemplate.opsForValue().set(RedisKeys.getRoomCodeMappingKey(room.getRoomCode()), room.getId().toString());
        
        if (oldRoomId != null) {
            stringRedisTemplate.opsForValue().set(RedisKeys.getRoomNextRoomKey(oldRoomId), room.getId().toString());
            sseStreamService.sendToRoom(oldRoomId, "NEW_LOBBY_CREATED", Map.of(
                "roomId", room.getId().toString(),
                "roomCode", room.getRoomCode()
            ));
        }
        
        Map<String, Object> state = new HashMap<>();
        state.put("roomId", room.getId().toString());
        state.put("roomCode", room.getRoomCode());
        state.put("state", "LOBBY");
        state.put("users", new ArrayList<>());
        state.put("leaderboard", new ArrayList<>());
        state.put("results", new ArrayList<>());
        
        try {
            stringRedisTemplate.opsForValue().set(RedisKeys.getRoomStateJson(room.getId()), objectMapper.writeValueAsString(state));
        } catch(Exception e) {}

        Map<String, Object> response = new HashMap<>();
        response.put("roomId", room.getId().toString());
        response.put("roomCode", room.getRoomCode());
        response.put("state", "LOBBY");
        response.put("targetScore", room.getTargetScore());
        response.put("targetQuestions", room.getTargetQuestions());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId:[0-9a-fA-F\\\\-]{36}}/start")
    public ResponseEntity<Void> startRoom(@PathVariable UUID roomId) {
        updateRoomState(roomId, "STARTING");
        String roomCode = getRoomCodeFromRedis(roomId);
        sseStreamService.sendToRoom(roomId, "STATE_CHANGE", Map.of("state", "STARTING", "roomId", roomId.toString(), "roomCode", roomCode));
        
        scheduledExecutorService.schedule(() -> {
            updateRoomState(roomId, "ACTIVE");
            sseStreamService.sendToRoom(roomId, "GAME_START", Map.of("state", "ACTIVE", "roomId", roomId.toString(), "roomCode", roomCode));
            sseStreamService.sendToRoom(roomId, "STATE_CHANGE", Map.of("state", "ACTIVE", "roomId", roomId.toString(), "roomCode", roomCode));
        }, 5, TimeUnit.SECONDS);

        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{roomId:[0-9a-fA-F\\\\-]{36}}/reset")
    public ResponseEntity<Void> resetRoom(@PathVariable UUID roomId) {
        updateRoomState(roomId, "LOBBY");
        stringRedisTemplate.delete(RedisKeys.getRoomScoresKey(roomId));
        stringRedisTemplate.delete(RedisKeys.getRoomLeaderboardKey(roomId));
        stringRedisTemplate.delete(RedisKeys.getRoomResultsKey(roomId));
        
        String roomCode = getRoomCodeFromRedis(roomId);
        sseStreamService.sendToRoom(roomId, "STATE_CHANGE", Map.of("state", "LOBBY", "roomId", roomId.toString(), "roomCode", roomCode));
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{roomId:[0-9a-fA-F\\\\-]{36}}/lobby-return")
    public ResponseEntity<Void> lobbyReturn(@PathVariable UUID roomId) {
        return resetRoom(roomId);
    }
    
    @PostMapping("/{roomId:[0-9a-fA-F\\\\-]{36}}/leave")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable UUID roomId,
            @RequestBody(required = false) Map<String, String> payload) {
        
        String nickname = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String && !authentication.getPrincipal().equals("anonymousUser")) {
            nickname = (String) authentication.getPrincipal(); 
        }
        
        if (nickname == null && payload != null && payload.containsKey("nickname")) {
            nickname = payload.get("nickname");
        }
        
        if (nickname == null || nickname.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<Participant> participants = participantRepository.findByRoomId(roomId);
        final String searchName = nickname;
        Participant participant = participants.stream()
                .filter(p -> p.getNickname().equals(searchName))
                .findFirst().orElse(null);
                
        if (participant == null) {
            return ResponseEntity.notFound().build();
        }
        
        participantRepository.delete(participant);
        
        // Broadcast roster update
        sseStreamService.sendToRoom(roomId, "ROSTER_UPDATE", Map.of("users", getActiveUsers(roomId)));
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roomId:[0-9a-fA-F\\\\-]{36}}/state")
    public ResponseEntity<Map<String, Object>> getRoomState(@PathVariable UUID roomId) {
        String stateJson = stringRedisTemplate.opsForValue().get(RedisKeys.getRoomStateJson(roomId));
        if (stateJson != null) {
            try {
                return ResponseEntity.ok(objectMapper.readValue(stateJson, Map.class));
            } catch(Exception e) {}
        }
        
        String state = stringRedisTemplate.opsForValue().get(RedisKeys.getRoomStateKey(roomId));
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        
        Room room = roomRepository.findById(roomId).orElse(null);
        Map<String, Object> response = new HashMap<>();
        response.put("roomId", roomId.toString());
        response.put("roomCode", room != null ? room.getRoomCode() : "");
        response.put("state", state);
        response.put("users", getActiveUsers(roomId)); 
        response.put("leaderboard", new ArrayList<>()); 
        response.put("results", new ArrayList<>());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{roomId:[0-9a-fA-F\\\\-]{36}}/next-room")
    public ResponseEntity<Map<String, Object>> getNextRoom(@PathVariable UUID roomId) {
        String nextRoomId = stringRedisTemplate.opsForValue().get(RedisKeys.getRoomNextRoomKey(roomId));
        Map<String, Object> response = new HashMap<>();
        if (nextRoomId != null) {
            response.put("hasNextRoom", true);
            response.put("roomId", nextRoomId);
            response.put("roomCode", getRoomCodeFromRedis(UUID.fromString(nextRoomId)));
        } else {
            response.put("hasNextRoom", false);
            response.put("roomId", null);
            response.put("roomCode", null);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ping")
    public ResponseEntity<Void> ping() {
        return ResponseEntity.ok().build();
    }
    
    private List<String> getActiveUsers(UUID roomId) {
        List<Participant> participants = participantRepository.findByRoomId(roomId);
        return participants.stream().map(Participant::getNickname).toList();
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

    private String getRoomCodeFromRedis(UUID roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        return room != null ? room.getRoomCode() : "";
    }

    private String generateUniqueRoomCode() {
        String code;
        do {
            code = generateRoomCode();
        } while (roomRepository.findByRoomCode(code).isPresent());
        return code;
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
