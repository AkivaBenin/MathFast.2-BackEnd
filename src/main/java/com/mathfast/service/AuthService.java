package com.mathfast.service;

import com.mathfast.dto.JoinRequestDto;
import com.mathfast.entity.Guest;
import com.mathfast.entity.Room;
import com.mathfast.entity.Teacher;
import com.mathfast.enums.GameState;
import com.mathfast.exception.RoomClosedException;
import com.mathfast.repository.ParticipantRepository;
import com.mathfast.repository.RoomRepository;
import com.mathfast.repository.TeacherRepository;
import com.mathfast.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TeacherRepository teacherRepository;
    private final ParticipantRepository participantRepository;
    private final RoomRepository roomRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Teacher authenticateTeacher(String username, String password) {
        Teacher teacher = teacherRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(password, teacher.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        return teacher;
    }

    @Transactional
    public Guest joinGuest(JoinRequestDto request) {
        Room room = roomRepository.findByRoomCode(request.getRoomCode())
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (room.getStatus() != GameState.LOBBY) {
            throw new RoomClosedException("Game already in progress");
        }

        // Nickname string inherently supports UTF-8 (and Hebrew characters)
        Guest guest = Guest.builder()
                .nickname(request.getNickname())
                .build();

        return participantRepository.save(guest);
    }
    
    public Room getRoomByCode(String roomCode) {
        return roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));
    }
}
