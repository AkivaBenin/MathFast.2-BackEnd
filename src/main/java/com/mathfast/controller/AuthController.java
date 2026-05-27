package com.mathfast.controller;

import com.mathfast.dto.JoinRequestDto;
import com.mathfast.dto.ParticipantDto;
import com.mathfast.entity.Guest;
import com.mathfast.entity.Room;
import com.mathfast.entity.Teacher;
import com.mathfast.exception.RoomClosedException;
import com.mathfast.mapper.EntityMapper;
import com.mathfast.security.JwtUtil;
import com.mathfast.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/teacher/login")
    public ResponseEntity<?> loginTeacher(@RequestBody com.mathfast.dto.TeacherAuthDto request) {
        Teacher teacher = authService.authenticateTeacher(request.getUsername(), request.getPassword());
        String token = jwtUtil.generateTeacherToken(teacher.getUsername(), "", teacher.getId().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/teacher/register")
    public ResponseEntity<?> registerTeacher(@RequestBody com.mathfast.dto.TeacherAuthDto request) {
        Teacher teacher = authService.registerTeacher(request.getUsername(), request.getPassword());
        String token = jwtUtil.generateTeacherToken(teacher.getUsername(), "", teacher.getId().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/guest/join")
    public ResponseEntity<?> joinGuest(@RequestBody JoinRequestDto request) {
        Guest guest = authService.joinGuest(request);
        Room room = authService.getRoomByCode(request.getRoomCode());

        String token = jwtUtil.generateGuestToken(guest.getNickname(), room.getId().toString(), guest.getId().toString());
        ParticipantDto participantDto = EntityMapper.toParticipantDto(guest);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("roomId", room.getId().toString());
        response.put("roomCode", room.getRoomCode());
        response.put("nickname", guest.getNickname());
        response.put("role", "GUEST");
        response.put("participant", participantDto);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<com.mathfast.dto.ApiResponse<Void>> logout() {
        return ResponseEntity.ok(com.mathfast.dto.ApiResponse.ok(null));
    }
}
