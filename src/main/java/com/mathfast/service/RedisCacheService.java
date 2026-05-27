package com.mathfast.service;

import com.mathfast.exception.ExploitDetectedException;
import com.mathfast.exception.RoomClosedException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final StringRedisTemplate redisTemplate;
    private DefaultRedisScript<String> validateAndScoreScript;

    @PostConstruct
    public void init() {
        validateAndScoreScript = new DefaultRedisScript<>();
        validateAndScoreScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/validate_and_score.lua")));
        validateAndScoreScript.setResultType(String.class);
    }

    public void processMove(UUID roomId, UUID playerId, String nonce, int points) {
        String roomStateKey = com.mathfast.constant.RedisKeys.getRoomStateKey(roomId);
        String nonceKey = com.mathfast.constant.RedisKeys.getNonceKey(nonce);
        String scoresKey = com.mathfast.constant.RedisKeys.getRoomScoresKey(roomId);

        List<String> keys = List.of(roomStateKey, nonceKey, scoresKey);

        String result = redisTemplate.execute(
                validateAndScoreScript,
                keys,
                nonce,
                String.valueOf(points),
                playerId.toString()
        );

        log.info("Score update result for player {}: {}", playerId, result);

        if ("ROOM_NOT_ACTIVE".equals(result)) {
            throw new RoomClosedException("The room is not active.");
        } else if ("NONCE_INVALID".equals(result)) {
            throw new ExploitDetectedException("Invalid or already used nonce detected.");
        } else if (!"SUCCESS".equals(result)) {
            throw new IllegalStateException("Unexpected result from Redis script: " + result);
        }
    }
}
