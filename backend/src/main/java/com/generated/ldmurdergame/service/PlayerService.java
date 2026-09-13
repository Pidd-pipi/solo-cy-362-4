package com.generated.ldmurdergame.service;

import com.generated.ldmurdergame.dto.CreatePlayerRequest;
import com.generated.ldmurdergame.entity.Player;
import com.generated.ldmurdergame.exception.ApiException;
import com.generated.ldmurdergame.mapper.PlayerMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerService {

  private final PlayerMapper playerMapper;

  public PlayerService(PlayerMapper playerMapper) {
    this.playerMapper = playerMapper;
  }

  @Transactional(readOnly = true)
  public List<Player> list() {
    return playerMapper.selectAll();
  }

  /** 玩家注册（演示用）：手机号唯一。 */
  @Transactional
  public Player create(CreatePlayerRequest request) {
    if (playerMapper.selectByPhone(request.phone()) != null) {
      throw new ApiException("该手机号已注册，请勿重复注册");
    }
    Player player = new Player();
    player.setName(request.name());
    player.setPhone(request.phone());
    player.setMemberLevel("青铜");
    player.setCreatedAt(LocalDateTime.now());
    playerMapper.insert(player);
    return playerMapper.selectById(player.getId());
  }
}
