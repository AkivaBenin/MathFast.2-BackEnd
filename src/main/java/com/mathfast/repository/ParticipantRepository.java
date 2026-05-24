package com.mathfast.repository;

import com.mathfast.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    @Query("SELECT p FROM Participant p WHERE p.room.id = :roomId")
    List<Participant> findByRoomId(@Param("roomId") UUID roomId);
}
