package com.generated.ldmurdergame.dto;

/** 报名/取消操作结果：让前端明确知道本次落到的状态与候补顺位 */
public record RegistrationActionResponse(
    Long registrationId,
    Long sessionId,
    Long playerId,
    String playerName,
    String result,
    String status,
    Integer waitlistPosition,
    Integer registeredCount,
    Integer waitlistCount,
    Integer remainingSlots,
    Long promotedPlayerId,
    String promotedPlayerName,
    String message
) {
}
