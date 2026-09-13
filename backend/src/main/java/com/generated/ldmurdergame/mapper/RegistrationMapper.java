package com.generated.ldmurdergame.mapper;

import com.generated.ldmurdergame.entity.Registration;
import com.generated.ldmurdergame.enums.RegistrationStatus;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RegistrationMapper {
  void insert(Registration registration);

  /** 取消后再次报名：复用历史行，重置状态/顺位/时间 */
  void reactivate(Registration registration);

  /** 查询某玩家在某场次的记录（含已取消），用于重复报名判定与行复用 */
  Registration selectBySessionAndPlayer(@Param("sessionId") Long sessionId,
                                        @Param("playerId") Long playerId);

  int countBySessionAndStatus(@Param("sessionId") Long sessionId,
                              @Param("status") RegistrationStatus status);

  /** 全部门店范围内某状态的报名数（总览指标用） */
  int countAllByStatus(@Param("status") RegistrationStatus status);

  /** 候补队列按顺位、报名时间排序 */
  List<Registration> selectWaitlist(@Param("sessionId") Long sessionId);

  /** 锁行读取首位候补（顺位最小，同分按最早报名时间） */
  Registration selectFirstWaitlistForUpdate(@Param("sessionId") Long sessionId);

  /** 场次内的有效报名：先已报名、再候补顺位 */
  List<Registration> selectActiveBySession(@Param("sessionId") Long sessionId);

  /** 玩家的全部有效报名（跨场次），按开场时间排序 */
  List<Registration> selectActiveByPlayer(@Param("playerId") Long playerId);

  void updateStatus(@Param("id") Long id,
                    @Param("status") RegistrationStatus status,
                    @Param("waitlistPosition") Integer waitlistPosition);

  /** 首位候补转正 */
  void promoteToRegistered(@Param("id") Long id);

  /** 顺位大于指定值的候补全部前移一位（取消/转正后保持顺位连续） */
  void decrementPositionsAfter(@Param("sessionId") Long sessionId,
                               @Param("position") Integer position);

  /** 门店取消场次时，连带取消全部有效报名 */
  int cancelAllActiveForSession(@Param("sessionId") Long sessionId);

  /** 场次开始时，未转正的候补全部取消 */
  int cancelWaitlistBySession(@Param("sessionId") Long sessionId);
}
