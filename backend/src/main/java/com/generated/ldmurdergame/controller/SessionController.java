package com.generated.ldmurdergame.controller;

import com.generated.ldmurdergame.dto.CreateSessionRequest;
import com.generated.ldmurdergame.dto.RegisterRequest;
import com.generated.ldmurdergame.dto.RegistrationActionResponse;
import com.generated.ldmurdergame.dto.RegistrationResponse;
import com.generated.ldmurdergame.dto.SessionResponse;
import com.generated.ldmurdergame.dto.SessionStatsResponse;
import com.generated.ldmurdergame.entity.GameSession;
import com.generated.ldmurdergame.enums.SessionStatus;
import com.generated.ldmurdergame.service.RegistrationService;
import com.generated.ldmurdergame.service.SessionService;
import com.generated.ldmurdergame.support.Responses;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

  private final SessionService sessionService;
  private final RegistrationService registrationService;

  public SessionController(SessionService sessionService,
                           RegistrationService registrationService) {
    this.sessionService = sessionService;
    this.registrationService = registrationService;
  }

  /** 场次列表，可按状态过滤：SCHEDULED / STARTED / CANCELLED。 */
  @GetMapping({"/sessions", "/api/sessions"})
  public List<SessionResponse> list(@RequestParam(required = false) SessionStatus status) {
    return sessionService.list(status).stream().map(Responses::of).toList();
  }

  /** 场次报名模块实时指标（门店运营总览入口）。 */
  @GetMapping({"/sessions/stats", "/api/sessions/stats"})
  public SessionStatsResponse stats() {
    return sessionService.stats();
  }

  @GetMapping({"/sessions/{id}", "/api/sessions/{id}"})
  public SessionResponse detail(@PathVariable Long id) {
    return Responses.of(sessionService.detail(id));
  }

  /** 门店创建场次：设置剧本、开始时间、人数上限。 */
  @PostMapping({"/sessions", "/api/sessions"})
  public SessionResponse create(@Valid @RequestBody CreateSessionRequest request) {
    GameSession session = sessionService.create(request);
    return Responses.of(session);
  }

  /** 门店开场。 */
  @PostMapping({"/sessions/{id}/start", "/api/sessions/{id}/start"})
  public SessionResponse start(@PathVariable Long id) {
    sessionService.start(id);
    return Responses.of(sessionService.detail(id));
  }

  /** 门店取消场次。 */
  @PostMapping({"/sessions/{id}/cancel", "/api/sessions/{id}/cancel"})
  public SessionResponse cancel(@PathVariable Long id) {
    sessionService.cancel(id);
    return Responses.of(sessionService.detail(id));
  }

  /** 候补队列（含顺位）。 */
  @GetMapping({"/sessions/{id}/waitlist", "/api/sessions/{id}/waitlist"})
  public List<RegistrationResponse> waitlist(@PathVariable Long id) {
    return registrationService.getWaitlist(id).stream().map(Responses::of).toList();
  }

  /** 场次全部有效报名（已报名 + 候补）。 */
  @GetMapping({"/sessions/{id}/registrations", "/api/sessions/{id}/registrations"})
  public List<RegistrationResponse> registrations(@PathVariable Long id) {
    return registrationService.getSessionRegistrations(id).stream().map(Responses::of).toList();
  }

  /** 玩家报名：名额未满直接报名，满员进入候补。 */
  @PostMapping({"/sessions/{id}/register", "/api/sessions/{id}/register"})
  public RegistrationActionResponse register(@PathVariable Long id,
                                             @Valid @RequestBody RegisterRequest request) {
    return registrationService.register(id, request.playerId());
  }

  /** 玩家取消报名；若让出名额，首位候补自动转正。 */
  @DeleteMapping({"/sessions/{id}/register", "/api/sessions/{id}/register"})
  public RegistrationActionResponse cancelRegistration(@PathVariable Long id,
                                                       @RequestParam Long playerId) {
    return registrationService.cancel(id, playerId);
  }
}
