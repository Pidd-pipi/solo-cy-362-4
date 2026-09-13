package com.generated.ldmurdergame.controller;

import com.generated.ldmurdergame.dto.ScriptResponse;
import com.generated.ldmurdergame.service.ScriptService;
import com.generated.ldmurdergame.support.Responses;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScriptController {

  private final ScriptService scriptService;

  public ScriptController(ScriptService scriptService) {
    this.scriptService = scriptService;
  }

  /** 剧本库（创建场次时选择剧本）。 */
  @GetMapping({"/scripts", "/api/scripts"})
  public List<ScriptResponse> list() {
    return scriptService.list().stream().map(Responses::of).toList();
  }
}
