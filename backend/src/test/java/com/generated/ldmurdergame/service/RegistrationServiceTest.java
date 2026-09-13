package com.generated.ldmurdergame.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link RegistrationService} 纯单元测试（不启动 Spring、不连数据库）。
 *
 * <p>每个用例都在独立 mock 上构建数据，相互之间没有任何共享状态，可任意次数重复运行。
 * 覆盖：报名中且未到点、报名中但已到点、已开始、已取消、名额未满、名额已满、
 * 取消后重新报名、占名额者取消触发首位候补转正、候补者取消触发顺位前移。
 * 每项均断言报名状态、候补顺位、剩余名额与错误提示。
 */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

  private static final long SESSION_ID = 7L;
  private static final long PLAYER_ID = 50L;
  private static final long PROMOTED_ID = 51L;
  private static final long REGISTRATION_ID = 900L;

  @Mock
  private GameSessionMapper sessionMapper;
  @Mock
  private RegistrationMapper registrationMapper;
  @Mock
  private PlayerMapper playerMapper;

  @InjectMocks
  private RegistrationService registrationService;

  // ---------- 工厂方法：每个用例独立对象，保证可重复运行 ----------

  private GameSession session(SessionStatus status, LocalDateTime startTime, int capacity) {
    GameSession session = new GameSession();
    session.setId(SESSION_ID);
    session.setScriptId(1L);
    session.setStatus(status);
    session.setStartTime(startTime);
    session.setCapacity(capacity);
    return session;
  }

  private Player player(long id, String name) {
    Player player = new Player();
    player.setId(id);
    player.setName(name);
    player.setPhone("1380000" + id);
    player.setMemberLevel("青铜");
    return player;
  }

  private Registration registration(long id, long playerId, RegistrationStatus status,
                                    Integer position) {
    Registration registration = new Registration();
    registration.setId(id);
    registration.setSessionId(SESSION_ID);
    registration.setPlayerId(playerId);
    registration.setStatus(status);
    registration.setWaitlistPosition(position);
    return registration;
  }

  /** 模拟 buildActionResponse 重新读取的场次聚合数据。 */
  private void stubDetail(int capacity, int registered, int waitlisted) {
    GameSession detail = session(SessionStatus.SCHEDULED, LocalDateTime.now().plusDays(1),
        capacity);
    detail.setRegisteredCount(registered);
    detail.setWaitlistCount(waitlisted);
    when(sessionMapper.selectDetailById(SESSION_ID)).thenReturn(detail);
  }

  /** 新插入的报名行回填自增 ID，模拟 MyBatis useGeneratedKeys 行为。 */
  private void stubGeneratedId() {
    org.mockito.Mockito.doAnswer(invocation -> {
      invocation.getArgument(0, Registration.class).setId(REGISTRATION_ID);
      return 1;
    }).when(registrationMapper).insert(any(Registration.class));
  }

  // ---------- 报名：状态 × 时间 ----------

  @Test
  void scheduledAndBeforeStart_withFreeSlot_registersDirectly() {
    GameSession locked = session(SessionStatus.SCHEDULED, LocalDateTime.now().plusHours(2), 6);
    when(sessionMapper.selectByIdForUpdate(SESSION_ID)).thenReturn(locked);
    when(playerMapper.selectById(PLAYER_ID)).thenReturn(player(PLAYER_ID, "玩家甲"));
    when(registrationMapper.selectBySessionAndPlayer(SESSION_ID, PLAYER_ID)).thenReturn(null);
    when(registrationMapper.countBySessionAndStatus(SESSION_ID, RegistrationStatus.REGISTERED))
        .thenReturn(2);
    stubGeneratedId();
    stubDetail(6, 3, 0);

    RegistrationActionResponse response = registrationService.register(SESSION_ID, PLAYER_ID);

    // 状态：直接报名；顺位：无；剩余名额：6-3=3；提示：报名成功
    assertThat(response.result()).isEqualTo("REGISTERED");
    assertThat(response.status()).isEqualTo("REGISTERED");
    assertThat(response.waitlistPosition()).isNull();
    assertThat(response.registeredCount()).isEqualTo(3);
    assertThat(response.waitlistCount()).isZero();
    assertThat(response.remainingSlots()).isEqualTo(3);
    assertThat(response.message()).isEqualTo("报名成功");

    ArgumentCaptor<Registration> captor = ArgumentCaptor.forClass(Registration.class);
    verify(registrationMapper).insert(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
    assertThat(captor.getValue().getWaitlistPosition()).isNull();
  }

  @Test
  void scheduledButStartTimeReached_rejectsEvenWithoutManualStart() {
    // 状态仍是报名中（门店未手动开场），但开始时间已到 -> 截止
    GameSession locked = session(SessionStatus.SCHEDULED, LocalDateTime.now().minusMinutes(1), 6);
    when(sessionMapper.selectByIdForUpdate(SESSION_ID)).thenReturn(locked);

    assertThatThrownBy(() -> registrationService.register(SESSION_ID, PLAYER_ID))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("已过开始时间")
        .hasMessageContaining("报名已截止");

    // 截止后不应产生任何报名/候补写入
    verify(registrationMapper, never()).insert(any());
    verify(registrationMapper, never()).reactivate(any());
  }

  @Test
  void startedSession_rejectsRegistration() {
    GameSession locked = session(SessionStatus.STARTED, LocalDateTime.now().minusHours(1), 6);
    when(sessionMapper.selectByIdForUpdate(SESSION_ID)).thenReturn(locked);

    assertThatThrownBy(() -> registrationService.register(SESSION_ID, PLAYER_ID))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("已开始");
    verify(registrationMapper, never()).insert(any());
  }

  @Test
  void cancelledSession_rejectsRegistration() {
    GameSession locked = session(SessionStatus.CANCELLED, LocalDateTime.now().plusDays(1), 6);
    when(sessionMapper.selectByIdForUpdate(SESSION_ID)).thenReturn(locked);

    assertThatThrownBy(() -> registrationService.register(SESSION_ID, PLAYER_ID))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("已取消");
    verify(registrationMapper, never()).insert(any());
  }

  // ---------- 报名：名额 ----------

  @Test
  void fullSession_putsNewPlayerOnWaitlistWithPositionAndZeroRemaining() {
    GameSession locked = session(SessionStatus.SCHEDULED, LocalDateTime.now().plusHours(3), 4);
    when(sessionMapper.selectByIdForUpdate(SESSION_ID)).thenReturn(locked);
    when(playerMapper.selectById(PLAYER_ID)).thenReturn(player(PLAYER_ID, "玩家乙"));
    when(registrationMapper.selectBySessionAndPlayer(SESSION_ID, PLAYER_ID)).thenReturn(null);
    // 已报名 4 人 == 名额，候补已有 2 人
    when(registrationMapper.countBySessionAndStatus(SESSION_ID, RegistrationStatus.REGISTERED))
        .thenReturn(4);
    when(registrationMapper.countBySessionAndStatus(SESSION_ID, RegistrationStatus.WAITLISTED))
        .thenReturn(2);
    stubGeneratedId();
    stubDetail(4, 4, 3);

    RegistrationActionResponse response = registrationService.register(SESSION_ID, PLAYER_ID);

    // 状态：候补；顺位：2+1=3；剩余名额：0；提示含顺位
    assertThat(response.result()).isEqualTo("WAITLISTED");
    assertThat(response.status()).isEqualTo("WAITLISTED");
    assertThat(response.waitlistPosition()).isEqualTo(3);
    assertThat(response.remainingSlots()).isZero();
    assertThat(response.registeredCount()).isEqualTo(4);
    assertThat(response.waitlistCount()).isEqualTo(3);
    assertThat(response.message()).contains("候补顺位第 3 位");

    ArgumentCaptor<Registration> captor = ArgumentCaptor.forClass(Registration.class);
    verify(registrationMapper).insert(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(RegistrationStatus.WAITLISTED);
    assertThat(captor.getValue().getWaitlistPosition()).isEqualTo(3);
  }

  @Test
  void alreadyRegistered_duplicateRegistrationRejectedWithClearMessage() {
    GameSession locked = session(SessionStatus.SCHEDULED, LocalDateTime.now().plusHours(3), 6);
    when(sessionMapper.selectByIdForUpdate(SESSION_ID)).thenReturn(locked);
    when(playerMapper.selectById(PLAYER_ID)).thenReturn(player(PLAYER_ID, "玩家甲"));
    when(registrationMapper.selectBySessionAndPlayer(SESSION_ID, PLAYER_ID))
        .thenReturn(registration(REGISTRATION_ID, PLAYER_ID, RegistrationStatus.REGISTERED, null));

    assertThatThrownBy(() -> registrationService.register(SESSION_ID, PLAYER_ID))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("已成功报名")
        .hasMessageContaining("重复报名");
    verify(registrationMapper, never()).insert(any());
    verify(registrationMapper, never()).reactivate(any());
  }

  @Test
  void alreadyWaitlisted_duplicateReportsCurrentPosition() {
    GameSession locked = session(SessionStatus.SCHEDULED, LocalDateTime.now().plusHours(3), 4);
    when(sessionMapper.selectByIdForUpdate(SESSION_ID)).thenReturn(locked);
    when(playerMapper.selectById(PLAYER_ID)).thenReturn(player(PLAYER_ID, "玩家乙"));
    when(registrationMapper.selectBySessionAndPlayer(SESSION_ID, PLAYER_ID))
        .thenReturn(registration(REGISTRATION_ID, PLAYER_ID, RegistrationStatus.WAITLISTED, 2));

    assertThatThrownBy(() -> registrationService.register(SESSION_ID, PLAYER_ID))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("候补队列")
        .hasMessageContaining("第 2 位");
  }

  // ---------- 取消后重新报名（历史行复用） ----------

  @Test
  void registerAfterCancellation_reactivatesRowAndRegistersWhenSlotFree() {
    GameSession locked = session(SessionStatus.SCHEDULED, LocalDateTime.now().plusHours(3), 6);
    when(sessionMapper.selectByIdForUpdate(SESSION_ID)).thenReturn(locked);
    when(playerMapper.selectById(PLAYER_ID)).thenReturn(player(PLAYER_ID, "玩家甲"));
    // 玩家此前取消过：存在 CANCELLED 历史行，应复用而非新插入
    Registration cancelled = registration(REGISTRATION_ID, PLAYER_ID,
        RegistrationStatus.CANCELLED, null);
    when(registrationMapper.selectBySessionAndPlayer(SESSION_ID, PLAYER_ID)).thenReturn(cancelled);
    when(registrationMapper.countBySessionAndStatus(SESSION_ID, RegistrationStatus.REGISTERED))
        .thenReturn(1);
    stubDetail(6, 2, 0);

    RegistrationActionResponse response = registrationService.register(SESSION_ID, PLAYER_ID);

    assertThat(response.registrationId()).isEqualTo(REGISTRATION_ID);
    assertThat(response.result()).isEqualTo("REGISTERED");
    assertThat(response.waitlistPosition()).isNull();
    assertThat(response.remainingSlots()).isEqualTo(4);
    // 复用历史行走 reactivate，绝不新增第二行
    verify(registrationMapper).reactivate(any(Registration.class));
    verify(registrationMapper, never()).insert(any());
    assertThat(cancelled.getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
  }

  @Test
  void registerAfterCancellation_whenFull_goesBackToWaitlistWithNewPosition() {
    GameSession locked = session(SessionStatus.SCHEDULED, LocalDateTime.now().plusHours(3), 4);
    when(sessionMapper.selectByIdForUpdate(SESSION_ID)).thenReturn(locked);
    when(playerMapper.selectById(PLAYER_ID)).thenReturn(player(PLAYER_ID, "玩家乙"));
    when(registrationMapper.selectBySessionAndPlayer(SESSION_ID, PLAYER_ID))
        .thenReturn(registration(REGISTRATION_ID, PLAYER_ID, RegistrationStatus.CANCELLED, null));
    when(registrationMapper.countBySessionAndStatus(SESSION_ID, RegistrationStatus.REGISTERED))
        .thenReturn(4);
    when(registrationMapper.countBySessionAndStatus(SESSION_ID, RegistrationStatus.WAITLISTED))
        .thenReturn(0);
    stubDetail(4, 4, 1);

    RegistrationActionResponse response = registrationService.register(SESSION_ID, PLAYER_ID);

    // 满员 -> 重新进入候补第 1 位，剩余名额 0
    assertThat(response.result()).isEqualTo("WAITLISTED");
    assertThat(response.waitlistPosition()).isEqualTo(1);
    assertThat(response.remainingSlots()).isZero();
    verify(registrationMapper).reactivate(any(Registration.class));
    verify(registrationMapper, never()).insert(any());
  }

  // ---------- 取消：占名额者取消 -> 首位候补转正 + 顺位前移 ----------

  @Test
  void cancelRegisteredPlayer_promotesFirstWaitlistedAndShiftsPositions() {
    GameSession locked = session(SessionStatus.SCHEDULED, LocalDateTime.now().plusHours(3), 2);
    when(sessionMapper.selectByIdForUpdate(SESSION_ID)).thenReturn(locked);
    when(playerMapper.selectById(PLAYER_ID)).thenReturn(player(PLAYER_ID, "占名额玩家"));
    Registration mine = registration(REGISTRATION_ID, PLAYER_ID, RegistrationStatus.REGISTERED, null);
    when(registrationMapper.selectBySessionAndPlayer(SESSION_ID, PLAYER_ID)).thenReturn(mine);
    // 首位候补（顺位1）存在，将转正
    Registration first = registration(901L, PROMOTED_ID, RegistrationStatus.WAITLISTED, 1);
    when(registrationMapper.selectFirstWaitlistForUpdate(SESSION_ID)).thenReturn(first);
    when(playerMapper.selectById(PROMOTED_ID)).thenReturn(player(PROMOTED_ID, "首位候补"));
    // 转正后聚合：已报名仍为 2，候补剩 1
    stubDetail(2, 2, 1);

    RegistrationActionResponse response = registrationService.cancel(SESSION_ID, PLAYER_ID);

    assertThat(response.result()).isEqualTo("CANCELLED");
    assertThat(response.status()).isEqualTo("CANCELLED");
    assertThat(response.waitlistPosition()).isNull();
    assertThat(response.promotedPlayerId()).isEqualTo(PROMOTED_ID);
    assertThat(response.promotedPlayerName()).isEqualTo("首位候补");
    assertThat(response.registeredCount()).isEqualTo(2);
    assertThat(response.waitlistCount()).isEqualTo(1);
    assertThat(response.remainingSlots()).isZero();
    assertThat(response.message()).contains("首位候补", "自动转正");

    // 自己的报名置为取消；首位候补行转正；顺位1之后全部前移
    verify(registrationMapper).updateStatus(REGISTRATION_ID, RegistrationStatus.CANCELLED, null);
    verify(registrationMapper).promoteToRegistered(901L);
    verify(registrationMapper).decrementPositionsAfter(SESSION_ID, 1);
  }

  // ---------- 取消：候补者取消 -> 其后顺位前移，不触发转正 ----------

  @Test
  void cancelWaitlistedPlayer_shiftsLaterPositionsWithoutPromotion() {
    GameSession locked = session(SessionStatus.SCHEDULED, LocalDateTime.now().plusHours(3), 2);
    when(sessionMapper.selectByIdForUpdate(SESSION_ID)).thenReturn(locked);
    when(playerMapper.selectById(PLAYER_ID)).thenReturn(player(PLAYER_ID, "候补玩家"));
    Registration mine = registration(REGISTRATION_ID, PLAYER_ID,
        RegistrationStatus.WAITLISTED, 2);
    when(registrationMapper.selectBySessionAndPlayer(SESSION_ID, PLAYER_ID)).thenReturn(mine);
    // 候补者退出：已报名人数不变（2），候补少 1
    stubDetail(2, 2, 1);

    RegistrationActionResponse response = registrationService.cancel(SESSION_ID, PLAYER_ID);

    assertThat(response.result()).isEqualTo("CANCELLED");
    assertThat(response.promotedPlayerId()).isNull();
    assertThat(response.promotedPlayerName()).isNull();
    assertThat(response.remainingSlots()).isZero();
    assertThat(response.registeredCount()).isEqualTo(2);
    assertThat(response.waitlistCount()).isEqualTo(1);
    assertThat(response.message()).isEqualTo("已取消报名");

    verify(registrationMapper).updateStatus(REGISTRATION_ID, RegistrationStatus.CANCELLED, null);
    // 不应查询/转正首位候补
    verify(registrationMapper, never()).selectFirstWaitlistForUpdate(any());
    verify(registrationMapper, never()).promoteToRegistered(any());
    // 顺位2之后全部前移
    verify(registrationMapper).decrementPositionsAfter(SESSION_ID, 2);
  }

  // ---------- 非法取消的明确提示 ----------

  @Test
  void cancelWithoutActiveRegistration_isRejected() {
    GameSession locked = session(SessionStatus.SCHEDULED, LocalDateTime.now().plusHours(3), 6);
    when(sessionMapper.selectByIdForUpdate(SESSION_ID)).thenReturn(locked);
    when(playerMapper.selectById(PLAYER_ID)).thenReturn(player(PLAYER_ID, "路人"));
    when(registrationMapper.selectBySessionAndPlayer(SESSION_ID, PLAYER_ID)).thenReturn(null);

    assertThatThrownBy(() -> registrationService.cancel(SESSION_ID, PLAYER_ID))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("没有该场次的有效报名");
    verify(registrationMapper, never()).updateStatus(any(), any(), any());
  }

  @Test
  void cancelOnStartedSession_isRejected() {
    when(sessionMapper.selectByIdForUpdate(SESSION_ID))
        .thenReturn(session(SessionStatus.STARTED, LocalDateTime.now().minusHours(1), 6));

    assertThatThrownBy(() -> registrationService.cancel(SESSION_ID, PLAYER_ID))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("已开始")
        .hasMessageContaining("不能取消报名");
    verifyNoInteractions(registrationMapper);
  }

  @Test
  void cancelOnCancelledSession_isRejected() {
    when(sessionMapper.selectByIdForUpdate(SESSION_ID))
        .thenReturn(session(SessionStatus.CANCELLED, LocalDateTime.now().plusDays(1), 6));

    assertThatThrownBy(() -> registrationService.cancel(SESSION_ID, PLAYER_ID))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("已被门店取消");
    verifyNoInteractions(registrationMapper);
  }

  @Test
  void registerIntoMissingSession_isRejected() {
    when(sessionMapper.selectByIdForUpdate(SESSION_ID)).thenReturn(null);

    assertThatThrownBy(() -> registrationService.register(SESSION_ID, PLAYER_ID))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("场次不存在");
    verifyNoInteractions(registrationMapper);
  }
}
