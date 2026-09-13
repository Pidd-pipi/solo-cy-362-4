package com.generated.ldmurdergame.dto;

import java.time.LocalDateTime;

public record ScriptResponse(
    Long id,
    String name,
    String genre,
    String difficulty,
    Integer durationMinutes,
    Integer minPlayers,
    Integer maxPlayers,
    Boolean dmRequired,
    String description,
    LocalDateTime createdAt
) {
}
