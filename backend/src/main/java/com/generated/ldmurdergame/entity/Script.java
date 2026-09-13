package com.generated.ldmurdergame.entity;

import java.time.LocalDateTime;

public class Script {
  private Long id;
  private String name;
  private String genre;
  private String difficulty;
  private Integer durationMinutes;
  private Integer minPlayers;
  private Integer maxPlayers;
  private Boolean dmRequired;
  private String description;
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

  public String getGenre() {
    return genre;
  }

  public void setGenre(String genre) {
    this.genre = genre;
  }

  public String getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(String difficulty) {
    this.difficulty = difficulty;
  }

  public Integer getDurationMinutes() {
    return durationMinutes;
  }

  public void setDurationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public Integer getMinPlayers() {
    return minPlayers;
  }

  public void setMinPlayers(Integer minPlayers) {
    this.minPlayers = minPlayers;
  }

  public Integer getMaxPlayers() {
    return maxPlayers;
  }

  public void setMaxPlayers(Integer maxPlayers) {
    this.maxPlayers = maxPlayers;
  }

  public Boolean getDmRequired() {
    return dmRequired;
  }

  public void setDmRequired(Boolean dmRequired) {
    this.dmRequired = dmRequired;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
