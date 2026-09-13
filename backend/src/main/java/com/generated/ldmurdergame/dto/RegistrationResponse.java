package com.generated.ldmurdergame.dto;

import java.time.LocalDateTime;

/** 报名记录（同时用于已报名列表与候补队列） */
public record RegistrationResponse(
    Long id,
    Long sessionId,
    String scriptName,
    LocalDateTime sessionStartTime,
    Integer capacity,
    String sessionStatus,
    Long playerId,
    String playerName,
    String playerPhone,
    String memberLevel,
    String status,
    Integer waitlistPosition,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
