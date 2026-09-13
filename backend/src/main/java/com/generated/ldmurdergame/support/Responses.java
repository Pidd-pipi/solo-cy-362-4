package com.generated.ldmurdergame.support;

import com.generated.ldmurdergame.dto.PlayerResponse;
import com.generated.ldmurdergame.dto.RegistrationResponse;
import com.generated.ldmurdergame.dto.ScriptResponse;
import com.generated.ldmurdergame.dto.SessionResponse;
import com.generated.ldmurdergame.entity.GameSession;
import com.generated.ldmurdergame.entity.Player;
import com.generated.ldmurdergame.entity.Registration;
import com.generated.ldmurdergame.entity.Script;

/** 实体 -> 响应 DTO 转换。 */
public final class Responses {

  private Responses() {
  }

  public static ScriptResponse of(Script script) {
    return new ScriptResponse(script.getId(), script.getName(), script.getGenre(),
        script.getDifficulty(), script.getDurationMinutes(), script.getMinPlayers(),
        script.getMaxPlayers(), script.getDmRequired(), script.getDescription(),
        script.getCreatedAt());
  }

  public static PlayerResponse of(Player player) {
    return new PlayerResponse(player.getId(), player.getName(), player.getPhone(),
        player.getMemberLevel(), player.getCreatedAt());
  }

  public static SessionResponse of(GameSession s) {
    int registered = s.getRegisteredCount() == null ? 0 : s.getRegisteredCount();
    int waitlist = s.getWaitlistCount() == null ? 0 : s.getWaitlistCount();
    int remaining = Math.max(0, s.getCapacity() - registered);
    boolean full = registered >= s.getCapacity();
    // 报名中且未到开始时间才可报名；开始时间到达后即使门店未手动开场也截止
    boolean registerable = s.isRegistrationOpen();
    return new SessionResponse(s.getId(), s.getScriptId(), s.getScriptName(), s.getGenre(),
        s.getDmName(), s.getStartTime(), s.getCapacity(), s.getStatus().name(),
        registered, waitlist, remaining, full, registerable, s.getCreatedAt());
  }

  public static RegistrationResponse of(Registration r) {
    return new RegistrationResponse(r.getId(), r.getSessionId(), r.getScriptName(),
        r.getSessionStartTime(), r.getCapacity(), r.getSessionStatus(), r.getPlayerId(),
        r.getPlayerName(), r.getPlayerPhone(), r.getMemberLevel(), r.getStatus().name(),
        r.getWaitlistPosition(), r.getCreatedAt(), r.getUpdatedAt());
  }
}
