package com.generated.ldmurdergame.mapper;

import com.generated.ldmurdergame.entity.Player;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlayerMapper {
  List<Player> selectAll();

  Player selectById(@Param("id") Long id);

  Player selectByPhone(@Param("phone") String phone);

  void insert(Player player);
}
