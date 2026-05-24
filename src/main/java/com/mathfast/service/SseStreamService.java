package com.mathfast.service;

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

    // Mapping Room ID -> (Player ID -> SseEmitter)
    private final Map<UUID, Map<UUID, SseEmitter>> roomEmitters = new ConcurrentHashMap<>();

    public SseEmitter createConnection(UUID roomId, UUID playerId) {
        SseEmitter emitter = new SseEmitter(2 * 60 * 60 * 1000L); // 2 hours timeout

        roomEmitters.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(playerId, emitter);

        emitter.onCompletion(() -> removeEmitter(roomId, playerId));
        emitter.onTimeout(() -> removeEmitter(roomId, playerId));
        emitter.onError((e) -> removeEmitter(roomId, playerId));

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

    public void broadcastRoomState(UUID roomId) {
        Map<UUID, SseEmitter> players = roomEmitters.get(roomId);
        if (players == null || players.isEmpty()) {
            return;
        }

        String roomStateJson = redisTemplate.opsForValue().get("room:" + roomId);
        if (roomStateJson == null) {
            log.warn("Room state for {} not found in Redis during broadcast", roomId);
            return;
        }

        for (Map.Entry<UUID, SseEmitter> playerEntry : players.entrySet()) {
            UUID playerId = playerEntry.getKey();
            SseEmitter emitter = playerEntry.getValue();

            try {
                emitter.send(SseEmitter.event().name("roomState").data(roomStateJson));
            } catch (IOException e) {
                log.warn("Failed to broadcast to player {} in room {}, closing connection.", playerId, roomId);
                emitter.complete();
                players.remove(playerId);
            }
        }
        if (players.isEmpty()) {
            roomEmitters.remove(roomId);
        }
    }
}
