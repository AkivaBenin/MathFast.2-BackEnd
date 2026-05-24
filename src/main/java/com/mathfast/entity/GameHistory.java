package com.mathfast.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "game_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false)
    private String roomCode;

    @Column(nullable = false)
    private String playerNickname;

    @Column(nullable = false)
    private Integer score;
}
