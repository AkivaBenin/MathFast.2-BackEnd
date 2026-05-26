package com.mathfast.controller;

import com.mathfast.dto.ApiResponse;
import com.mathfast.dto.MoveRequestDto;
import com.mathfast.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/race")
@RequiredArgsConstructor
public class MoveController {

    private final RedisCacheService redisCacheService;

    @PostMapping("/{roomId}/move")
    public ResponseEntity<ApiResponse<Void>> makeMove(
            @PathVariable UUID roomId,
            @RequestBody MoveRequestDto moveRequest) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID playerId = UUID.nameUUIDFromBytes(authentication.getName().getBytes());

        int points = switch (moveRequest.getDifficulty()) {
            case "HIGHWAY" -> 30;
            case "DIRT_ROAD" -> 15;
            default -> 10;
        };

        redisCacheService.processMove(
                roomId,
                playerId,
                moveRequest.getNonce(),
                points
        );

        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
