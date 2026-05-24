package com.mathfast.controller;

import com.mathfast.service.SseStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/race")
@RequiredArgsConstructor
public class RaceController {

    private final SseStreamService sseStreamService;

    @GetMapping(value = "/{roomId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRoomEvents(@PathVariable UUID roomId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Extract deterministic player ID derived from the JWT subject (username/nickname)
        UUID playerId = UUID.nameUUIDFromBytes(authentication.getName().getBytes());
        
        return sseStreamService.createConnection(roomId, playerId);
    }
}
