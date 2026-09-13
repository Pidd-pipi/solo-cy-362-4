package com.generated.ldmurdergame.dto;

import java.time.LocalDateTime;

/** 场次列表/详情项：名额、状态、候补人数一并返回 */
public record SessionResponse(
    Long id,
    Long scriptId,
    String scriptName,
    String genre,
    String dmName,
    LocalDateTime startTime,
    Integer capacity,
    String status,
    Integer registeredCount,
    Integer waitlistCount,
    Integer remainingSlots,
    Boolean full,
    Boolean registerable,
    LocalDateTime createdAt
) {
}
