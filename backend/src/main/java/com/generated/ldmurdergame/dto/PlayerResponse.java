package com.generated.ldmurdergame.dto;

import java.time.LocalDateTime;

public record PlayerResponse(
    Long id,
    String name,
    String phone,
    String memberLevel,
    LocalDateTime createdAt
) {
}
