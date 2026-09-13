package com.generated.ldmurdergame.entity;

import com.generated.ldmurdergame.enums.SessionStatus;
import java.time.LocalDateTime;

public class GameSession {
  private Long id;
  private Long scriptId;
  private String dmName;
  private LocalDateTime startTime;
  private Integer capacity;
  private SessionStatus status;
  private LocalDateTime createdAt;

  // 列表/详情联表与聚合字段
  private String scriptName;
  private String genre;
  private Integer registeredCount;
  private Integer waitlistCount;

  /**
   * 是否仍可报名：场次处于报名中，且开始时间未到。
   * 开始时间到达后即使门店尚未手动开场，报名与候补也立即截止。
   */
  public boolean isRegistrationOpen() {
    return status == SessionStatus.SCHEDULED
        && startTime != null
        && LocalDateTime.now().isBefore(startTime);
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getScriptId() {
    return scriptId;
  }

  public void setScriptId(Long scriptId) {
    this.scriptId = scriptId;
  }

  public String getDmName() {
    return dmName;
  }

  public void setDmName(String dmName) {
    this.dmName = dmName;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public SessionStatus getStatus() {
    return status;
  }

  public void setStatus(SessionStatus status) {
    this.status = status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public String getScriptName() {
    return scriptName;
  }

  public void setScriptName(String scriptName) {
    this.scriptName = scriptName;
  }

  public String getGenre() {
    return genre;
  }

  public void setGenre(String genre) {
    this.genre = genre;
  }

  public Integer getRegisteredCount() {
    return registeredCount;
  }

  public void setRegisteredCount(Integer registeredCount) {
    this.registeredCount = registeredCount;
  }

  public Integer getWaitlistCount() {
    return waitlistCount;
  }

  public void setWaitlistCount(Integer waitlistCount) {
    this.waitlistCount = waitlistCount;
  }
}
