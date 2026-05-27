package com.mathfast.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathfast.constant.RedisKeys;
import com.mathfast.dto.QuestionDto;
import com.mathfast.entity.Room;
import com.mathfast.repository.RoomRepository;
import com.mathfast.service.MathEngineService;
import com.mathfast.service.SseStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/race")
@RequiredArgsConstructor
public class RaceController {

    private final SseStreamService sseStreamService;
    private final MathEngineService mathEngineService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RoomRepository roomRepository;

    @GetMapping(value = "/{roomId:[0-9a-fA-F\\\\-]{36}}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRoomEvents(@PathVariable UUID roomId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID playerId = UUID.fromString(authentication.getCredentials().toString());
        
        return sseStreamService.createConnection(roomId, playerId);
    }

    @GetMapping("/{roomId:[0-9a-fA-F\\\\-]{36}}/question")
    public ResponseEntity<QuestionDto> getQuestion(
            @PathVariable UUID roomId,
            @RequestParam(required = false) com.mathfast.enums.Path requestedPath) {
        
        com.mathfast.enums.Path path = requestedPath != null ? requestedPath : com.mathfast.enums.Path.REGULAR;
        QuestionDto questionDto = mathEngineService.generateQuestion(path, false);
        
        stringRedisTemplate.opsForValue().set(
                RedisKeys.getNonceKey(questionDto.getNonce()), 
                String.valueOf(questionDto.getSolution()), 
                30, 
                TimeUnit.SECONDS
        );
        
        questionDto.setSolution(null);
        
        return ResponseEntity.ok(questionDto);
    }

    @GetMapping("/{roomId:[0-9a-fA-F\\\\-]{36}}/results")
    public ResponseEntity<Map<String, Object>> getResults(@PathVariable UUID roomId) {
        String resultsJson = stringRedisTemplate.opsForValue().get(RedisKeys.getRoomResultsKey(roomId));
        List<Map<String, Object>> resultsList = new ArrayList<>();
        if (resultsJson != null) {
            try {
                resultsList = objectMapper.readValue(resultsJson, new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {}
        }
        
        Room room = roomRepository.findById(roomId).orElse(null);
        String roomCode = room != null ? room.getRoomCode() : "";
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("roomId", roomId.toString());
        response.put("roomCode", roomCode);
        response.put("results", resultsList);
        
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.PostMapping("/{roomId:[0-9a-fA-F\\\\-]{36}}/sabotage")
    public ResponseEntity<?> sabotage(
            @PathVariable UUID roomId,
            @org.springframework.web.bind.annotation.RequestBody(required = false) java.util.Map<String, Object> payload) {
        
        int participantCount = sseStreamService.getConnectedCount(roomId);
        if (participantCount <= 1) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("solo", true);
            response.put("pointsGained", 15);
            return ResponseEntity.ok(response);
        } else {
            sseStreamService.sendToRoom(roomId, "PLAYER_SABOTAGED", payload != null ? payload : new java.util.HashMap<>());
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("solo", false);
            return ResponseEntity.ok(response);
        }
    }
}
