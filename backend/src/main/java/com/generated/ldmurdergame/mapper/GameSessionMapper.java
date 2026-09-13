package com.generated.ldmurdergame.mapper;

import com.generated.ldmurdergame.entity.GameSession;
import com.generated.ldmurdergame.enums.SessionStatus;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GameSessionMapper {
  void insert(GameSession session);

  /** 行锁读取，报名/取消/状态流转都先锁场次行，串行化同场次并发操作 */
  GameSession selectByIdForUpdate(@Param("id") Long id);

  /** 联表读取剧本信息与实时报名/候补人数 */
  GameSession selectDetailById(@Param("id") Long id);

  List<GameSession> selectList(@Param("status") SessionStatus status);

  void updateStatus(@Param("id") Long id, @Param("status") SessionStatus status);
}
