package com.generated.ldmurdergame.service;

import com.generated.ldmurdergame.dto.RegistrationActionResponse;
import com.generated.ldmurdergame.entity.GameSession;
import com.generated.ldmurdergame.entity.Player;
import com.generated.ldmurdergame.entity.Registration;
import com.generated.ldmurdergame.enums.RegistrationStatus;
import com.generated.ldmurdergame.enums.SessionStatus;
import com.generated.ldmurdergame.exception.ApiException;
import com.generated.ldmurdergame.mapper.GameSessionMapper;
import com.generated.ldmurdergame.mapper.PlayerMapper;
import com.generated.ldmurdergame.mapper.RegistrationMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 报名与候补核心逻辑。
 *
 * <p>并发保证：每个写操作都在事务内先 {@code SELECT ... FOR UPDATE} 锁定场次行，
 * 同一场次的报名/取消被数据库串行化；再配合 registrations(session_id, player_id)
 * 唯一约束兜底，确保“同一玩家一条有效报名”和“转正不超卖”在并发抢位时依然成立。
 */
@Service
public class RegistrationService {

  private final GameSessionMapper sessionMapper;
  private final RegistrationMapper registrationMapper;
  private final PlayerMapper playerMapper;

  public RegistrationService(GameSessionMapper sessionMapper,
                             RegistrationMapper registrationMapper,
                             PlayerMapper playerMapper) {
    this.sessionMapper = sessionMapper;
    this.registrationMapper = registrationMapper;
    this.playerMapper = playerMapper;
  }

  /**
   * 玩家报名：名额未满直接报名；满员进入候补，顺位按报名时间递增。
   *
   * <p>报名需同时满足：场次为报名中状态、当前时间早于开始时间。开始时间到达后，
   * 即使门店尚未手动开场，报名与候补也一律截止；已取消场次同样拒绝。
   */
  @Transactional
  public RegistrationActionResponse register(Long sessionId,
                                             Long playerId) {
    GameSession session = lockSession(sessionId);
    if (session.getStatus() == SessionStatus.CANCELLED) {
      throw new ApiException("该场次已取消，无法报名");
    }
    if (session.getStatus() == SessionStatus.STARTED) {
      throw new ApiException("该场次已开始，无法报名");
    }
    if (session.getStartTime() != null && !LocalDateTime.now().isBefore(session.getStartTime())) {
      throw new ApiException("该场次已过开始时间，报名已截止，无法报名或候补");
    }
    Player player = playerMapper.selectById(playerId);
    if (player == null) {
      throw new ApiException("玩家不存在，请先注册后再报名");
    }

    Registration existing = registrationMapper.selectBySessionAndPlayer(sessionId, playerId);
    if (existing != null && existing.getStatus() != RegistrationStatus.CANCELLED) {
      if (existing.getStatus() == RegistrationStatus.REGISTERED) {
        throw new ApiException("你已成功报名该场次，请勿重复报名");
      }
      throw new ApiException("你已在该场次候补队列中，当前候补顺位第 "
          + existing.getWaitlistPosition() + " 位，请勿重复报名");
    }

    int registeredCount = registrationMapper.countBySessionAndStatus(
        sessionId, RegistrationStatus.REGISTERED);
    LocalDateTime now = LocalDateTime.now();

    RegistrationStatus status;
    Integer position = null;
    String result;
    String message;
    if (registeredCount < session.getCapacity()) {
      status = RegistrationStatus.REGISTERED;
      result = "REGISTERED";
      message = "报名成功";
    } else {
      int waitlistCount = registrationMapper.countBySessionAndStatus(
          sessionId, RegistrationStatus.WAITLISTED);
      position = waitlistCount + 1;
      status = RegistrationStatus.WAITLISTED;
      result = "WAITLISTED";
      message = "本场次名额已满，你已进入候补队列，当前候补顺位第 " + position + " 位";
    }

    Long registrationId;
    if (existing == null) {
      Registration registration = new Registration();
      registration.setSessionId(sessionId);
      registration.setPlayerId(playerId);
      registration.setStatus(status);
      registration.setWaitlistPosition(position);
      registration.setCreatedAt(now);
      registration.setUpdatedAt(now);
      registrationMapper.insert(registration);
      registrationId = registration.getId();
    } else {
      existing.setStatus(status);
      existing.setWaitlistPosition(position);
      existing.setCreatedAt(now);
      existing.setUpdatedAt(now);
      registrationMapper.reactivate(existing);
      registrationId = existing.getId();
    }

    return buildActionResponse(registrationId, sessionId, player, result, status, position,
        message, null, null);
  }

  /**
   * 玩家取消报名：
   * 已报名者取消 → 候补首位自动转正，其后顺位前移；候补者取消 → 其后顺位前移。
   */
  @Transactional
  public RegistrationActionResponse cancel(Long sessionId,
                                                                          Long playerId) {
    GameSession session = lockSession(sessionId);
    if (session.getStatus() == SessionStatus.CANCELLED) {
      throw new ApiException("该场次已被门店取消，报名已自动失效，无需重复取消");
    }
    if (session.getStatus() == SessionStatus.STARTED) {
      throw new ApiException("该场次已开始，不能取消报名");
    }
    Player player = playerMapper.selectById(playerId);
    if (player == null) {
      throw new ApiException("玩家不存在，取消失败");
    }
    Registration existing = registrationMapper.selectBySessionAndPlayer(sessionId, playerId);
    if (existing == null || existing.getStatus() == RegistrationStatus.CANCELLED) {
      throw new ApiException("你没有该场次的有效报名，取消失败");
    }

    boolean wasWaitlisted = existing.getStatus() == RegistrationStatus.WAITLISTED;
    Integer cancelledPosition = existing.getWaitlistPosition();
    registrationMapper.updateStatus(existing.getId(), RegistrationStatus.CANCELLED, null);

    Long promotedId = null;
    String promotedName = null;
    Integer shiftAfter = null;
    String message = "已取消报名";

    if (wasWaitlisted) {
      // 候补者退出：其后顺位全部前移一位
      shiftAfter = cancelledPosition;
    } else {
      // 占名额者退出：首位候补转正，其余候补顺位前移
      Registration first = registrationMapper.selectFirstWaitlistForUpdate(sessionId);
      if (first != null) {
        registrationMapper.promoteToRegistered(first.getId());
        Player promoted = playerMapper.selectById(first.getPlayerId());
        promotedId = first.getPlayerId();
        promotedName = promoted == null ? null : promoted.getName();
        shiftAfter = 1;
        message = "已取消报名，候补第 1 位玩家「" + promotedName + "」已自动转正";
      }
    }
    if (shiftAfter != null) {
      registrationMapper.decrementPositionsAfter(sessionId, shiftAfter);
    }

    return buildActionResponse(existing.getId(), sessionId, player, "CANCELLED",
        RegistrationStatus.CANCELLED, null, message, promotedId, promotedName);
  }

  /** 候补队列（按顺位、报名时间）。 */
  @Transactional(readOnly = true)
  public List<Registration> getWaitlist(Long sessionId) {
    requireSession(sessionId);
    return registrationMapper.selectWaitlist(sessionId);
  }

  /** 场次内全部有效报名（已报名在前，候补按顺位）。 */
  @Transactional(readOnly = true)
  public List<Registration> getSessionRegistrations(Long sessionId) {
    requireSession(sessionId);
    return registrationMapper.selectActiveBySession(sessionId);
  }

  /** 玩家跨场次的有效报名。 */
  @Transactional(readOnly = true)
  public List<Registration> getPlayerRegistrations(Long playerId) {
    Player player = playerMapper.selectById(playerId);
    if (player == null) {
      throw new ApiException("玩家不存在");
    }
    return registrationMapper.selectActiveByPlayer(playerId);
  }

  private GameSession lockSession(Long sessionId) {
    GameSession session = sessionMapper.selectByIdForUpdate(sessionId);
    if (session == null) {
      throw new ApiException("场次不存在");
    }
    return session;
  }

  private void requireSession(Long sessionId) {
    GameSession session = sessionMapper.selectDetailById(sessionId);
    if (session == null) {
      throw new ApiException("场次不存在");
    }
  }

  private RegistrationActionResponse buildActionResponse(
      Long registrationId, Long sessionId, Player player, String result,
      RegistrationStatus status, Integer position, String message,
      Long promotedPlayerId, String promotedPlayerName) {
    GameSession detail = sessionMapper.selectDetailById(sessionId);
    int registered = detail.getRegisteredCount() == null ? 0 : detail.getRegisteredCount();
    int waitlist = detail.getWaitlistCount() == null ? 0 : detail.getWaitlistCount();
    int remaining = Math.max(0, detail.getCapacity() - registered);
    return new RegistrationActionResponse(
        registrationId, sessionId, player.getId(), player.getName(), result, status.name(),
        position, registered, waitlist, remaining,
        promotedPlayerId, promotedPlayerName, message);
  }
}
