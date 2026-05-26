package com.mathfast.controller;

import com.mathfast.dto.QuestionDto;
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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/race")
@RequiredArgsConstructor
public class RaceController {

    private final SseStreamService sseStreamService;
    private final MathEngineService mathEngineService;
    private final StringRedisTemplate stringRedisTemplate;

    @GetMapping(value = "/{roomId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRoomEvents(@PathVariable UUID roomId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID playerId = UUID.nameUUIDFromBytes(authentication.getName().getBytes());
        
        return sseStreamService.createConnection(roomId, playerId);
    }

    @GetMapping("/{roomId}/question")
    public ResponseEntity<QuestionDto> getQuestion(
            @PathVariable UUID roomId,
            @RequestParam(required = false) com.mathfast.enums.Path requestedPath) {
        
        com.mathfast.enums.Path path = requestedPath != null ? requestedPath : com.mathfast.enums.Path.REGULAR;
        QuestionDto questionDto = mathEngineService.generateQuestion(path, false);
        
        stringRedisTemplate.opsForValue().set(
                "nonce:" + questionDto.getNonce(), 
                questionDto.getNonce(), 
                30, 
                TimeUnit.SECONDS
        );
        
        return ResponseEntity.ok(questionDto);
    }
}
