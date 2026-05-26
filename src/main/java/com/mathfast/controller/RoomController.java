package com.mathfast.controller;

import com.mathfast.dto.RoomDto;
import com.mathfast.entity.Room;
import com.mathfast.mapper.EntityMapper;
import com.mathfast.repository.RoomRepository;
import com.mathfast.service.SseStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomRepository roomRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ScheduledExecutorService scheduledExecutorService;
    private final SseStreamService sseStreamService;

    @PostMapping
    public ResponseEntity<RoomDto> createRoom() {
        Room room = new Room();
        room = roomRepository.save(room);
        
        stringRedisTemplate.opsForValue().set("room:" + room.getId() + ":state", "LOBBY");
        
        RoomDto roomDto = EntityMapper.toRoomDto(room);
        return ResponseEntity.ok(roomDto);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Void> startRoom(@PathVariable UUID id) {
        stringRedisTemplate.opsForValue().set("room:" + id + ":state", "STARTING");
        sseStreamService.broadcastRoomState(id);
        
        scheduledExecutorService.schedule(() -> {
            stringRedisTemplate.opsForValue().set("room:" + id + ":state", "ACTIVE");
            sseStreamService.broadcastRoomState(id);
        }, 5, TimeUnit.SECONDS);

        return ResponseEntity.ok().build();
    }
}
