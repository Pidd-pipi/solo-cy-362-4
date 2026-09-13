package com.generated.ldmurdergame.dto;

/** 场次模块核心指标（门店运营总览入口展示实时数据） */
public record SessionStatsResponse(
    long totalSessions,
    long scheduledSessions,
    long startedSessions,
    long cancelledSessions,
    long totalRegistered,
    long totalWaitlisted,
    long fullSessions,
    long availableSessions,
    double averageFillRate
) {
}
