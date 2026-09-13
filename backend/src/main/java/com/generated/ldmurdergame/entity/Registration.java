package com.generated.ldmurdergame.entity;

import com.generated.ldmurdergame.enums.RegistrationStatus;
import java.time.LocalDateTime;

public class Registration {
  private Long id;
  private Long sessionId;
  private Long playerId;
  private RegistrationStatus status;
  private Integer waitlistPosition;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // 联表展示字段
  private String playerName;
  private String playerPhone;
  private String memberLevel;
  private String scriptName;
  private LocalDateTime sessionStartTime;
  private Integer capacity;
  private String sessionStatus;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getSessionId() {
    return sessionId;
  }

  public void setSessionId(Long sessionId) {
    this.sessionId = sessionId;
  }

  public Long getPlayerId() {
    return playerId;
  }

  public void setPlayerId(Long playerId) {
    this.playerId = playerId;
  }

  public RegistrationStatus getStatus() {
    return status;
  }

  public void setStatus(RegistrationStatus status) {
    this.status = status;
  }

  public Integer getWaitlistPosition() {
    return waitlistPosition;
  }

  public void setWaitlistPosition(Integer waitlistPosition) {
    this.waitlistPosition = waitlistPosition;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getPlayerName() {
    return playerName;
  }

  public void setPlayerName(String playerName) {
    this.playerName = playerName;
  }

  public String getPlayerPhone() {
    return playerPhone;
  }

  public void setPlayerPhone(String playerPhone) {
    this.playerPhone = playerPhone;
  }

  public String getMemberLevel() {
    return memberLevel;
  }

  public void setMemberLevel(String memberLevel) {
    this.memberLevel = memberLevel;
  }

  public String getScriptName() {
    return scriptName;
  }

  public void setScriptName(String scriptName) {
    this.scriptName = scriptName;
  }

  public LocalDateTime getSessionStartTime() {
    return sessionStartTime;
  }

  public void setSessionStartTime(LocalDateTime sessionStartTime) {
    this.sessionStartTime = sessionStartTime;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public String getSessionStatus() {
    return sessionStatus;
  }

  public void setSessionStatus(String sessionStatus) {
    this.sessionStatus = sessionStatus;
  }
}
