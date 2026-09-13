package com.generated.ldmurdergame;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * 报名截止时间真实接口测试。
 *
 * <p>开始时间到达后，即使门店尚未手动开场（场次状态仍是 SCHEDULED），报名与候补
 * 也必须停止；registerable 标记同步失效。覆盖：开始前、时间刚到达、已超过、
 * 未手动开场、已取消五类场景。
 *
 * <p>“已过期”场次通过 JdbcTemplate 直接构造（门店创建接口本就禁止把开始时间
 * 设在过去）；“时间刚到达”场景则创建一个几秒后开场的场次，轮询观察截止点跨越。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RegistrationCutoffIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate rest;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  private String api(String path) {
    return "http://localhost:" + port + path;
  }

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private JsonNode read(ResponseEntity<String> response) {
    try {
      return json.readTree(response.getBody());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /** 通过门店接口创建未来场次。 */
  private long createFutureSession(LocalDateTime startTime, int capacity) {
    Map<String, Object> body = Map.of(
        "scriptId", 5,
        "dmName", "DM 截止测试",
        "startTime", startTime.truncatedTo(ChronoUnit.SECONDS).toString(),
        "capacity", capacity,
        "operator", "门店管理员");
    ResponseEntity<String> response = rest.postForEntity(
        api("/api/sessions"), new HttpEntity<>(body, jsonHeaders()), String.class);
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    return read(response).get("id").asLong();
  }

  /** 绕过门店校验，直接构造一个 SCHEDULED 但开始时间已过去的场次（模拟未手动开场）。 */
  private long insertPastSession(LocalDateTime startTime, int capacity) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      var ps = connection.prepareStatement(
          "INSERT INTO sessions (script_id, dm_name, start_time, capacity, status, created_at) "
              + "VALUES (5, 'DM 截止测试', ?, ?, 'SCHEDULED', CURRENT_TIMESTAMP)",
          new String[] { "id" });
      ps.setObject(1, startTime);
      ps.setInt(2, capacity);
      return ps;
    }, keyHolder);
    return keyHolder.getKey().longValue();
  }

  private JsonNode getSession(long sessionId) {
    return read(rest.getForEntity(api("/api/sessions/" + sessionId), String.class));
  }

  private ResponseEntity<String> register(long sessionId, long playerId) {
    return rest.postForEntity(api("/api/sessions/" + sessionId + "/register"),
        new HttpEntity<>(Map.of("playerId", playerId), jsonHeaders()), String.class);
  }

  @Test
  void registrationBeforeStartTimeSucceedsAndMarkedRegisterable() {
    long sessionId = createFutureSession(LocalDateTime.now().plusDays(1), 6);

    JsonNode detail = getSession(sessionId);
    assertThat(detail.get("status").asText()).isEqualTo("SCHEDULED");
    assertThat(detail.get("registerable").asBoolean()).isTrue();

    // 开始前：有名额直接报名成功
    JsonNode result = read(register(sessionId, 6));
    assertThat(result.get("result").asText()).isEqualTo("REGISTERED");
  }

  @Test
  void registrationClosesExactlyWhenStartTimeArrivesWithoutManualStart() throws Exception {
    // 开始时间设在 3 秒后，门店全程不调用开场接口
    long sessionId = createFutureSession(LocalDateTime.now().plusSeconds(3), 4);
    assertThat(getSession(sessionId).get("registerable").asBoolean()).isTrue();

    // 轮询等待时间线越过开始时刻（最多 15 秒）
    boolean closed = false;
    for (int i = 0; i < 30; i++) {
      Thread.sleep(500);
      if (!getSession(sessionId).get("registerable").asBoolean()) {
        closed = true;
        break;
      }
    }
    assertThat(closed).as("开始时间到达后 registerable 应自动失效").isTrue();

    // 未手动开场：状态仍是 SCHEDULED，但报名已截止
    JsonNode detail = getSession(sessionId);
    assertThat(detail.get("status").asText()).isEqualTo("SCHEDULED");
    assertThat(detail.get("registerable").asBoolean()).isFalse();

    // 即使仍有空位，也不能报名
    ResponseEntity<String> response = register(sessionId, 6);
    assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    assertThat(read(response).get("message").asText()).contains("已过开始时间", "报名已截止");
  }

  @Test
  void sessionPastStartTimeButNotManuallyStartedRejectsRegistrationAndWaitlist() {
    // 开始时间已超过 1 小时，门店未手动开场（状态仍为报名中）
    long sessionId = insertPastSession(LocalDateTime.now().minusHours(1), 4);

    JsonNode detail = getSession(sessionId);
    assertThat(detail.get("status").asText()).isEqualTo("SCHEDULED");
    assertThat(detail.get("registerable").asBoolean()).isFalse();
    // 名额为空也不允许进入候补——截止优先于满员候补规则
    assertThat(detail.get("remainingSlots").asInt()).isEqualTo(4);

    ResponseEntity<String> response = register(sessionId, 6);
    assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    JsonNode body = read(response);
    assertThat(body.get("message").asText()).contains("已过开始时间");
    assertThat(body.has("result")).isFalse();
  }

  @Test
  void fullWaitlistAlsoBlockedAfterStartTime() {
    // 开始时间已过 1 天：同样不能借候补通道报名
    long sessionId = insertPastSession(LocalDateTime.now().minusDays(1), 2);

    ResponseEntity<String> response = register(sessionId, 6);
    assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    assertThat(read(response).get("message").asText())
        .contains("已过开始时间")
        .doesNotContain("候补顺位");
  }

  @Test
  void cancelledSessionIsNotRegisterableRegardlessOfStartTime() {
    long sessionId = createFutureSession(LocalDateTime.now().plusDays(2), 6);
    assertThat(getSession(sessionId).get("registerable").asBoolean()).isTrue();

    ResponseEntity<String> cancelCall = rest.exchange(
        api("/api/sessions/" + sessionId + "/cancel"),
        HttpMethod.POST,
        new HttpEntity<Void>(jsonHeaders()), String.class);
    assertThat(cancelCall.getStatusCode().is2xxSuccessful()).isTrue();

    JsonNode detail = getSession(sessionId);
    assertThat(detail.get("status").asText()).isEqualTo("CANCELLED");
    assertThat(detail.get("registerable").asBoolean()).isFalse();

    ResponseEntity<String> response = register(sessionId, 6);
    assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    assertThat(read(response).get("message").asText()).contains("已取消");
  }

  @Test
  void manuallyStartedSeedSessionIsNotRegisterable() {
    // 种子场次 4：昨天开场且门店已手动开始
    JsonNode started = getSession(4);
    assertThat(started.get("status").asText()).isEqualTo("STARTED");
    assertThat(started.get("registerable").asBoolean()).isFalse();
  }
}
