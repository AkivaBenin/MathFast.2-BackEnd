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
                .orElseThrow(() -> new com.mathfast.exception.InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(password, teacher.getPasswordHash())) {
            throw new com.mathfast.exception.InvalidCredentialsException("Invalid credentials");
        }

        return teacher;
    }

    @Transactional
    public Guest joinGuest(JoinRequestDto request) {
        Room room = roomRepository.findByRoomCode(request.getRoomCode())
                .orElseThrow(() -> new RoomClosedException("Room not found"));

        if (room.getStatus() != GameState.LOBBY) {
            throw new RoomClosedException("Game already in progress");
        }

        // Nickname string inherently supports UTF-8 (and Hebrew characters)
        Guest guest = Guest.builder()
                .nickname(request.getNickname())
                .room(room)
                .build();

        return participantRepository.save(guest);
    }
    
    @Transactional
    public Teacher registerTeacher(String username, String password) {
        if (teacherRepository.findByUsername(username).isPresent()) {
            throw new com.mathfast.exception.UsernameAlreadyExistsException("Username already exists");
        }
        Teacher teacher = Teacher.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .nickname(username)
                .build();
        return teacherRepository.save(teacher);
    }

    public Room getRoomByCode(String roomCode) {
        return roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));
    }
}

