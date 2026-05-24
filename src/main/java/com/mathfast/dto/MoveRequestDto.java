package com.mathfast.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveRequestDto {
    private UUID playerId;
    private String nonce;
    private Integer answer;
    private String difficulty;
}
