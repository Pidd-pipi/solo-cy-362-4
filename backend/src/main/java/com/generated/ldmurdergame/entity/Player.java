package com.generated.ldmurdergame.entity;

import java.time.LocalDateTime;

public class Player {
  private Long id;
  private String name;
  private String phone;
  private String memberLevel;
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getMemberLevel() {
    return memberLevel;
  }

  public void setMemberLevel(String memberLevel) {
    this.memberLevel = memberLevel;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
