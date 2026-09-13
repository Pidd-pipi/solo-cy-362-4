package com.generated.ldmurdergame.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** 门店创建场次入参 */
public record CreateSessionRequest(
    @NotNull(message = "剧本不能为空")
    Long scriptId,

    String dmName,

    @NotNull(message = "开始时间不能为空")
    LocalDateTime startTime,

    @NotNull(message = "人数上限不能为空")
    @Min(value = 1, message = "人数上限至少为 1")
    @Max(value = 50, message = "人数上限不能超过 50")
    Integer capacity,

    @NotBlank(message = "操作人不能为空")
    String operator
) {
}
