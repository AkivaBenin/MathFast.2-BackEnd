package com.mathfast.dto;

import com.mathfast.enums.GameState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDto {
    private UUID id;
    private String roomCode;
    private GameState status;
    private int targetScore;
}
