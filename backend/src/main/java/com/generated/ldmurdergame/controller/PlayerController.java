package com.generated.ldmurdergame.controller;

import com.generated.ldmurdergame.dto.CreatePlayerRequest;
import com.generated.ldmurdergame.dto.PlayerResponse;
import com.generated.ldmurdergame.dto.RegistrationResponse;
import com.generated.ldmurdergame.service.PlayerService;
import com.generated.ldmurdergame.service.RegistrationService;
import com.generated.ldmurdergame.support.Responses;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlayerController {

  private final PlayerService playerService;
  private final RegistrationService registrationService;

  public PlayerController(PlayerService playerService,
                          RegistrationService registrationService) {
    this.playerService = playerService;
    this.registrationService = registrationService;
  }

  /** 玩家列表（报名表单选择玩家身份）。 */
  @GetMapping({"/players", "/api/players"})
  public List<PlayerResponse> list() {
    return playerService.list().stream().map(Responses::of).toList();
  }

  /** 注册新玩家。 */
  @PostMapping({"/players", "/api/players"})
  public PlayerResponse create(@Valid @RequestBody CreatePlayerRequest request) {
    return Responses.of(playerService.create(request));
  }

  /** 我的报名：跨场次的有效报名与候补顺位。 */
  @GetMapping({"/players/{id}/registrations", "/api/players/{id}/registrations"})
  public List<RegistrationResponse> registrations(@PathVariable Long id) {
    return registrationService.getPlayerRegistrations(id).stream().map(Responses::of).toList();
  }
}
