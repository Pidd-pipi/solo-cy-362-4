package com.generated.ldmurdergame.dto;

import jakarta.validation.constraints.NotNull;

/** 玩家报名入参 */
public record RegisterRequest(
    @NotNull(message = "玩家不能为空")
    Long playerId
) {
}
