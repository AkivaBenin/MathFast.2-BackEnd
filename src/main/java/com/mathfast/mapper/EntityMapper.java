package com.mathfast.mapper;

import com.mathfast.dto.ParticipantDto;
import com.mathfast.dto.RoomDto;
import com.mathfast.entity.Guest;
import com.mathfast.entity.Participant;
import com.mathfast.entity.Room;
import com.mathfast.entity.Teacher;

public class EntityMapper {

    private EntityMapper() {
        // Prevent instantiation of static utility class
    }

    public static RoomDto toRoomDto(Room room) {
        if (room == null) {
            return null;
        }

        return RoomDto.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .status(room.getStatus())
                .targetScore(room.getTargetScore() != null ? room.getTargetScore() : 0)
                .build();
    }

    public static ParticipantDto toParticipantDto(Participant participant) {
        if (participant == null) {
            return null;
        }

        String role = null;
        if (participant instanceof Teacher) {
            role = "TEACHER";
        } else if (participant instanceof Guest) {
            role = "GUEST";
        }

        return ParticipantDto.builder()
                .id(participant.getId())
                .nickname(participant.getNickname())
                .role(role)
                .score(0)
                .build();
    }
}
