package com.generated.ldmurdergame.service;

import com.generated.ldmurdergame.entity.Script;
import com.generated.ldmurdergame.mapper.ScriptMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScriptService {

  private final ScriptMapper scriptMapper;

  public ScriptService(ScriptMapper scriptMapper) {
    this.scriptMapper = scriptMapper;
  }

  @Transactional(readOnly = true)
  public List<Script> list() {
    return scriptMapper.selectAll();
  }
}
