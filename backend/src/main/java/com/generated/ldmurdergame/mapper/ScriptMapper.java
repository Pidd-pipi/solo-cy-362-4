package com.generated.ldmurdergame.mapper;

import com.generated.ldmurdergame.entity.Script;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScriptMapper {
  List<Script> selectAll();

  Script selectById(@Param("id") Long id);

  void insert(Script script);
}
