package com.mathfast.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathfast.constant.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseStreamService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // Mapping Room ID -> (Player ID -> SseEmitter)
    private final Map<UUID, Map<UUID, SseEmitter>> roomEmitters = new ConcurrentHashMap<>();

    public SseEmitter createConnection(UUID roomId, UUID playerId) {
        SseEmitter emitter = new SseEmitter(2 * 60 * 60 * 1000L); // 2 hours timeout

        roomEmitters.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(playerId, emitter);

        emitter.onCompletion(() -> removeEmitter(roomId, playerId));
        emitter.onTimeout(() -> removeEmitter(roomId, playerId));
        emitter.onError((e) -> removeEmitter(roomId, playerId));

        // When a user connects, we can optionally send them the current state.
        sendInitialState(emitter, roomId);

        return emitter;
    }

    private void removeEmitter(UUID roomId, UUID playerId) {
        Map<UUID, SseEmitter> playerEmitters = roomEmitters.get(roomId);
        if (playerEmitters != null) {
            playerEmitters.remove(playerId);
            if (playerEmitters.isEmpty()) {
                roomEmitters.remove(roomId);
            }
        }
    }

    @Scheduled(fixedRate = 15000)
    public void sendKeepAlive() {
        for (Map.Entry<UUID, Map<UUID, SseEmitter>> roomEntry : roomEmitters.entrySet()) {
            UUID roomId = roomEntry.getKey();
            Map<UUID, SseEmitter> players = roomEntry.getValue();

            for (Map.Entry<UUID, SseEmitter> playerEntry : players.entrySet()) {
                UUID playerId = playerEntry.getKey();
                SseEmitter emitter = playerEntry.getValue();

                try {
                    emitter.send(SseEmitter.event().comment("keepalive"));
                } catch (IOException e) {
                    log.warn("Failed to send keepalive to player {} in room {}, closing connection.", playerId, roomId);
                    emitter.complete();
                    players.remove(playerId);
                }
            }
            if (players.isEmpty()) {
                roomEmitters.remove(roomId);
            }
        }
    }

    private void sendInitialState(SseEmitter emitter, UUID roomId) {
        try {
            String roomStateJson = redisTemplate.opsForValue().get(RedisKeys.getRoomStateJson(roomId));
            if (roomStateJson != null) {
                Map<String, Object> stateMap = objectMapper.readValue(roomStateJson, Map.class);
                emitter.send(SseEmitter.event().name("STATE_CHANGE").data(stateMap));
            }
        } catch (Exception e) {
            log.warn("Could not send initial state to emitter for room {}", roomId);
        }
    }

    public void sendToRoom(UUID roomId, String eventName, Object data) {
        Map<UUID, SseEmitter> players = roomEmitters.get(roomId);
        if (players != null) {
            for (Map.Entry<UUID, SseEmitter> entry : players.entrySet()) {
                try {
                    entry.getValue().send(SseEmitter.event().name(eventName).data(data));
                } catch (IOException e) {
                    entry.getValue().complete();
                    players.remove(entry.getKey());
                }
            }
        }
    }

    public int getConnectedCount(UUID roomId) {
        Map<UUID, SseEmitter> players = roomEmitters.get(roomId);
        return players != null ? players.size() : 0;
    }
}
