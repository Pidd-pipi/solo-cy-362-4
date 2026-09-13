package com.generated.ldmurdergame.service;

import com.generated.ldmurdergame.dto.CreateSessionRequest;
import com.generated.ldmurdergame.dto.SessionStatsResponse;
import com.generated.ldmurdergame.entity.GameSession;
import com.generated.ldmurdergame.entity.Script;
import com.generated.ldmurdergame.enums.RegistrationStatus;
import com.generated.ldmurdergame.enums.SessionStatus;
import com.generated.ldmurdergame.exception.ApiException;
import com.generated.ldmurdergame.mapper.GameSessionMapper;
import com.generated.ldmurdergame.mapper.RegistrationMapper;
import com.generated.ldmurdergame.mapper.ScriptMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 场次排期：门店创建场次、开场、取消场次，以及场次总览指标。 */
@Service
public class SessionService {

  private final GameSessionMapper sessionMapper;
  private final ScriptMapper scriptMapper;
  private final RegistrationMapper registrationMapper;

  public SessionService(GameSessionMapper sessionMapper,
                        ScriptMapper scriptMapper,
                        RegistrationMapper registrationMapper) {
    this.sessionMapper = sessionMapper;
    this.scriptMapper = scriptMapper;
    this.registrationMapper = registrationMapper;
  }

  /** 门店创建场次：指定剧本、开始时间与人数上限。 */
  @Transactional
  public GameSession create(CreateSessionRequest request) {
    Script script = scriptMapper.selectById(request.scriptId());
    if (script == null) {
      throw new ApiException("所选剧本不存在");
    }
    if (request.startTime().isBefore(LocalDateTime.now())) {
      throw new ApiException("开始时间不能早于当前时间");
    }
    if (request.capacity() < script.getMinPlayers()) {
      throw new ApiException("人数上限不能低于剧本《" + script.getName() + "》的最低人数 "
          + script.getMinPlayers() + " 人");
    }
    if (request.capacity() > script.getMaxPlayers()) {
      throw new ApiException("人数上限不能超过剧本《" + script.getName() + "》的最高人数 "
          + script.getMaxPlayers() + " 人");
    }

    GameSession session = new GameSession();
    session.setScriptId(request.scriptId());
    session.setDmName(request.dmName());
    session.setStartTime(request.startTime());
    session.setCapacity(request.capacity());
    session.setStatus(SessionStatus.SCHEDULED);
    session.setCreatedAt(LocalDateTime.now());
    sessionMapper.insert(session);
    return sessionMapper.selectDetailById(session.getId());
  }

  @Transactional(readOnly = true)
  public List<GameSession> list(SessionStatus status) {
    return sessionMapper.selectList(status);
  }

  @Transactional(readOnly = true)
  public GameSession detail(Long id) {
    GameSession session = sessionMapper.selectDetailById(id);
    if (session == null) {
      throw new ApiException("场次不存在");
    }
    return session;
  }

  /** 门店开场：仅报名中场次可开场，开场后未转正的候补自动取消。 */
  @Transactional
  public void start(Long id) {
    GameSession session = sessionMapper.selectByIdForUpdate(id);
    if (session == null) {
      throw new ApiException("场次不存在");
    }
    if (session.getStatus() != SessionStatus.SCHEDULED) {
      throw new ApiException(session.getStatus() == SessionStatus.STARTED
          ? "该场次已开始，请勿重复操作"
          : "该场次已取消，不能开始");
    }
    sessionMapper.updateStatus(id, SessionStatus.STARTED);
    registrationMapper.cancelWaitlistBySession(id);
  }

  /** 门店取消场次：连带取消全部报名与候补。 */
  @Transactional
  public void cancel(Long id) {
    GameSession session = sessionMapper.selectByIdForUpdate(id);
    if (session == null) {
      throw new ApiException("场次不存在");
    }
    if (session.getStatus() == SessionStatus.CANCELLED) {
      throw new ApiException("该场次已取消，请勿重复操作");
    }
    if (session.getStatus() == SessionStatus.STARTED) {
      throw new ApiException("该场次已开始，不能取消");
    }
    sessionMapper.updateStatus(id, SessionStatus.CANCELLED);
    registrationMapper.cancelAllActiveForSession(id);
  }

  /** 场次模块实时指标，供门店运营总览展示。 */
  @Transactional(readOnly = true)
  public SessionStatsResponse stats() {
    List<GameSession> all = sessionMapper.selectList(null);
    long scheduled = all.stream().filter(s -> s.getStatus() == SessionStatus.SCHEDULED).count();
    long started = all.stream().filter(s -> s.getStatus() == SessionStatus.STARTED).count();
    long cancelled = all.stream().filter(s -> s.getStatus() == SessionStatus.CANCELLED).count();
    // “可报名/满员”指标只统计报名仍开放的场次（报名中且未到开始时间）
    List<GameSession> openSessions = all.stream()
        .filter(GameSession::isRegistrationOpen)
        .toList();
    long full = openSessions.stream()
        .filter(s -> s.getRegisteredCount() >= s.getCapacity())
        .count();
    long available = openSessions.size() - full;
    double fillRate = all.stream()
        .filter(s -> s.getCapacity() != null && s.getCapacity() > 0)
        .mapToDouble(s -> Math.min(1.0, s.getRegisteredCount() * 1.0 / s.getCapacity()))
        .average()
        .orElse(0.0);
    return new SessionStatsResponse(
        all.size(),
        scheduled,
        started,
        cancelled,
        registrationMapper.countAllByStatus(RegistrationStatus.REGISTERED),
        registrationMapper.countAllByStatus(RegistrationStatus.WAITLISTED),
        full,
        available,
        Math.round(fillRate * 1000) / 10.0);
  }
}
