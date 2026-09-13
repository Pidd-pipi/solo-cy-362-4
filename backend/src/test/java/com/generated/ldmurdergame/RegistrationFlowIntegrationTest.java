package com.generated.ldmurdergame;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

/**
 * 场次报名 / 候补队列真实接口测试。
 *
 * <p>通过真实 HTTP（随机端口 + 内嵌 H2）覆盖：
 * 名额未满直接报名、满员进入候补、重复报名拦截、取消与非法取消、
 * 有人取消后首位候补自动转正（名额/状态/顺位同步）、已开始场次禁报，
 * 以及 20 人对 4 个名额的并发抢位（恰好 4 人成功、其余全部候补且不超卖）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RegistrationFlowIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate rest;

  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  private String api(String path) {
    return "http://localhost:" + port + path;
  }

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  /** 创建一个 N 人、D 天后开场的全新场次（剧本5 支持 2 人起开），返回场次 ID。 */
  private long createSession(int capacity, int daysFromNow) {
    Map<String, Object> body = Map.of(
        "scriptId", 5,
        "dmName", "DM 测试",
        "startTime", LocalDateTime.now().plusDays(daysFromNow).truncatedTo(ChronoUnit.SECONDS)
            .toString(),
        "capacity", capacity,
        "operator", "门店管理员");
    ResponseEntity<String> response = rest.postForEntity(
        api("/api/sessions"), new HttpEntity<>(body, jsonHeaders()), String.class);
    assertThat(response.getStatusCode().is2xxSuccessful()).as(body.toString()).isTrue();
    return read(response).get("id").asLong();
  }

  private JsonNode read(ResponseEntity<String> response) {
    try {
      return json.readTree(response.getBody());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private ResponseEntity<String> register(long sessionId, long playerId) {
    return rest.postForEntity(api("/api/sessions/" + sessionId + "/register"),
        new HttpEntity<>(Map.of("playerId", playerId), jsonHeaders()), String.class);
  }

  private ResponseEntity<String> cancel(long sessionId, long playerId) {
    return rest.exchange(
        api("/api/sessions/" + sessionId + "/register?playerId=" + playerId),
        HttpMethod.DELETE, HttpEntity.EMPTY, String.class);
  }

  @Test
  void overviewAndSeededSessionsAreAvailable() {
    ResponseEntity<String> overview = rest.getForEntity(api("/api/overview"), String.class);
    assertThat(overview.getStatusCode().is2xxSuccessful()).isTrue();

    JsonNode sessions = read(rest.getForEntity(api("/api/sessions"), String.class));
    assertThat(sessions.isArray()).isTrue();
    assertThat(sessions.size()).isGreaterThanOrEqualTo(4);

    // 种子场次 1：名额 6，已报 4，剩 2，可报名
    JsonNode session1 = read(rest.getForEntity(api("/api/sessions/1"), String.class));
    assertThat(session1.get("capacity").asInt()).isEqualTo(6);
    assertThat(session1.get("registeredCount").asInt()).isEqualTo(4);
    assertThat(session1.get("waitlistCount").asInt()).isZero();
    assertThat(session1.get("remainingSlots").asInt()).isEqualTo(2);
    assertThat(session1.get("full").asBoolean()).isFalse();
    assertThat(session1.get("registerable").asBoolean()).isTrue();

    // 种子场次 2：名额 4，已报 4，满员，2 人候补
    JsonNode session2 = read(rest.getForEntity(api("/api/sessions/2"), String.class));
    assertThat(session2.get("full").asBoolean()).isTrue();
    assertThat(session2.get("remainingSlots").asInt()).isZero();
    assertThat(session2.get("waitlistCount").asInt()).isEqualTo(2);

    JsonNode waitlist = read(rest.getForEntity(api("/api/sessions/2/waitlist"), String.class));
    assertThat(waitlist.size()).isEqualTo(2);
    assertThat(waitlist.get(0).get("playerId").asLong()).isEqualTo(3L);
    assertThat(waitlist.get(0).get("waitlistPosition").asInt()).isEqualTo(1);
    assertThat(waitlist.get(1).get("playerId").asLong()).isEqualTo(4L);
    assertThat(waitlist.get(1).get("waitlistPosition").asInt()).isEqualTo(2);
  }

  @Test
  void openSlotsRegisterDirectlyAndDuplicateRegistrationIsRejected() {
    long sessionId = createSession(5, 10);

    // 名额未满：玩家 5 直接报名成功并占用名额
    ResponseEntity<String> first = register(sessionId, 5);
    assertThat(first.getStatusCode().is2xxSuccessful()).isTrue();
    JsonNode result = read(first);
    assertThat(result.get("result").asText()).isEqualTo("REGISTERED");
    assertThat(result.get("status").asText()).isEqualTo("REGISTERED");
    assertThat(result.get("registeredCount").asInt()).isEqualTo(1);
    assertThat(result.get("remainingSlots").asInt()).isEqualTo(4);

    // 同一玩家重复报名：明确提示且不产生第二条记录
    ResponseEntity<String> duplicate = register(sessionId, 5);
    assertThat(duplicate.getStatusCode().is4xxClientError()).isTrue();
    assertThat(read(duplicate).get("message").asText()).contains("已成功报名", "重复报名");

    JsonNode session = read(rest.getForEntity(api("/api/sessions/" + sessionId), String.class));
    assertThat(session.get("registeredCount").asInt()).isEqualTo(1);
  }

  @Test
  void fullSessionPutsPlayersOnWaitlistInOrder() {
    long sessionId = createSession(2, 11);

    // 玩家 5、6 抢满 2 个名额
    assertThat(read(register(sessionId, 5)).get("result").asText()).isEqualTo("REGISTERED");
    assertThat(read(register(sessionId, 6)).get("result").asText()).isEqualTo("REGISTERED");

    // 玩家 1、2 依次进入候补，顺位按报名时间
    JsonNode w1 = read(register(sessionId, 1));
    assertThat(w1.get("result").asText()).isEqualTo("WAITLISTED");
    assertThat(w1.get("waitlistPosition").asInt()).isEqualTo(1);
    assertThat(w1.get("message").asText()).contains("候补顺位第 1 位");

    JsonNode w2 = read(register(sessionId, 2));
    assertThat(w2.get("waitlistPosition").asInt()).isEqualTo(2);

    // 候补玩家重复报名：提示其当前顺位
    ResponseEntity<String> duplicate = register(sessionId, 1);
    assertThat(duplicate.getStatusCode().is4xxClientError()).isTrue();
    assertThat(read(duplicate).get("message").asText()).contains("候补队列", "第 1 位");
  }

  @Test
  void cancelByRegisteredPlayerPromotesFirstWaitlistedAndReordersPositions() {
    long sessionId = createSession(2, 12);

    // 2 个名额：5、6 报名成功；1、2、3 进入候补 1/2/3
    assertThat(read(register(sessionId, 5)).get("result").asText()).isEqualTo("REGISTERED");
    assertThat(read(register(sessionId, 6)).get("result").asText()).isEqualTo("REGISTERED");
    assertThat(read(register(sessionId, 1)).get("waitlistPosition").asInt()).isEqualTo(1);
    assertThat(read(register(sessionId, 2)).get("waitlistPosition").asInt()).isEqualTo(2);
    assertThat(read(register(sessionId, 3)).get("waitlistPosition").asInt()).isEqualTo(3);

    // 占名额玩家 5 取消：候补首位（玩家1）自动转正，玩家2/3 顺位前移为 1/2
    JsonNode cancelResult = read(cancel(sessionId, 5));
    assertThat(cancelResult.get("result").asText()).isEqualTo("CANCELLED");
    assertThat(cancelResult.get("promotedPlayerId").asLong()).isEqualTo(1L);
    assertThat(cancelResult.get("registeredCount").asInt()).isEqualTo(2);
    assertThat(cancelResult.get("waitlistCount").asInt()).isEqualTo(2);
    assertThat(cancelResult.get("remainingSlots").asInt()).isZero();
    assertThat(cancelResult.get("message").asText()).contains("自动转正");

    JsonNode registrations = read(
        rest.getForEntity(api("/api/sessions/" + sessionId + "/registrations"), String.class));
    List<JsonNode> registered = new ArrayList<>();
    Map<Long, Integer> positions = new java.util.HashMap<>();
    for (JsonNode r : registrations) {
      if (r.get("status").asText().equals("REGISTERED")) {
        registered.add(r);
      } else {
        positions.put(r.get("playerId").asLong(), r.get("waitlistPosition").asInt());
      }
    }
    assertThat(registered).hasSize(2);
    assertThat(registered.stream().map(r -> r.get("playerId").asLong()).toList())
        .containsExactlyInAnyOrder(6L, 1L);
    assertThat(positions).containsEntry(2L, 1).containsEntry(3L, 2);

    // 玩家 5 取消后再次报名：满员 → 重新进入候补（历史行复用，无重复行）
    JsonNode reRegister = read(register(sessionId, 5));
    assertThat(reRegister.get("result").asText()).isEqualTo("WAITLISTED");
    assertThat(reRegister.get("waitlistPosition").asInt()).isEqualTo(3);

    // 非法取消：再次取消玩家 5 的旧报名（当前是候补则先取消，再取消应报错）
    assertThat(cancel(sessionId, 5).getStatusCode().is2xxSuccessful()).isTrue();
    ResponseEntity<String> illegalCancel = cancel(sessionId, 5);
    assertThat(illegalCancel.getStatusCode().is4xxClientError()).isTrue();
    assertThat(read(illegalCancel).get("message").asText()).contains("没有该场次的有效报名");

    // 候补玩家 2 取消：其后顺位（玩家3）前移
    JsonNode cancelWaitlisted = read(cancel(sessionId, 2));
    assertThat(cancelWaitlisted.get("promotedPlayerId").isNull()).isTrue();
    JsonNode waitlist = read(
        rest.getForEntity(api("/api/sessions/" + sessionId + "/waitlist"), String.class));
    assertThat(waitlist).hasSize(1);
    assertThat(waitlist.get(0).get("playerId").asLong()).isEqualTo(3L);
    assertThat(waitlist.get(0).get("waitlistPosition").asInt()).isEqualTo(1);
  }

  @Test
  void startedAndCancelledSessionsRejectRegistration() {
    // 已开始的种子场次 4 不可报名
    ResponseEntity<String> started = register(4, 5);
    assertThat(started.getStatusCode().is4xxClientError()).isTrue();
    assertThat(read(started).get("message").asText()).contains("已开始");

    long sessionId = createSession(3, 13);
    // 门店取消场次后，玩家报名被拒
    ResponseEntity<String> cancelCall = rest.exchange(
        api("/api/sessions/" + sessionId + "/cancel"), HttpMethod.POST,
        new HttpEntity<Void>(jsonHeaders()), String.class);
    assertThat(cancelCall.getStatusCode().is2xxSuccessful()).isTrue();

    ResponseEntity<String> registerAfterCancel = register(sessionId, 5);
    assertThat(registerAfterCancel.getStatusCode().is4xxClientError()).isTrue();
    assertThat(read(registerAfterCancel).get("message").asText()).contains("已取消");

    // 已取消场次重复取消也会报错
    ResponseEntity<String> doubleCancel = rest.exchange(
        api("/api/sessions/" + sessionId + "/cancel"), HttpMethod.POST,
        new HttpEntity<Void>(jsonHeaders()), String.class);
    assertThat(doubleCancel.getStatusCode().is4xxClientError()).isTrue();
  }

  @Test
  void startingSessionSucceedsAndThenRejectsRegistration() {
    long sessionId = createSession(4, 14);
    assertThat(register(sessionId, 5).getStatusCode().is2xxSuccessful()).isTrue();

    ResponseEntity<String> start = rest.exchange(
        api("/api/sessions/" + sessionId + "/start"), HttpMethod.POST,
        new HttpEntity<Void>(jsonHeaders()), String.class);
    assertThat(start.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(read(start).get("status").asText()).isEqualTo("STARTED");

    ResponseEntity<String> lateRegister = register(sessionId, 6);
    assertThat(lateRegister.getStatusCode().is4xxClientError()).isTrue();
    assertThat(read(lateRegister).get("message").asText()).contains("已开始");

    ResponseEntity<String> lateCancel = cancel(sessionId, 5);
    assertThat(lateCancel.getStatusCode().is4xxClientError()).isTrue();
    assertThat(read(lateCancel).get("message").asText()).contains("已开始");
  }

  @Test
  void concurrentRegistrationFillsExactlyCapacityWithoutOverselling() throws Exception {
    int capacity = 4;
    int players = 20;
    long sessionId = createSession(capacity, 15);

    // 通过真实接口注册 20 名玩家（门店端不预置玩家时也能跑）
    List<Long> playerIds = new ArrayList<>();
    for (int i = 1; i <= players; i++) {
      String phone = String.format("139%08d", i);
      JsonNode created = read(rest.postForEntity(api("/api/players"),
          new HttpEntity<>(Map.of("name", "并发玩家" + i, "phone", phone), jsonHeaders()),
          String.class));
      playerIds.add(created.get("id").asLong());
    }
    assertThat(playerIds).hasSize(players);

    // 20 个不同玩家同时对 4 个名额发起报名
    ExecutorService pool = Executors.newFixedThreadPool(players);
    CountDownLatch ready = new CountDownLatch(players);
    CountDownLatch go = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(players);
    AtomicInteger registered = new AtomicInteger();
    AtomicInteger waitlisted = new AtomicInteger();
    AtomicInteger errors = new AtomicInteger();
    List<Integer> waitPositions = Collections.synchronizedList(new ArrayList<>());
    Set<Long> registeredIds = Collections.synchronizedSet(new HashSet<>());

    for (int i = 0; i < players; i++) {
      long playerId = playerIds.get(i);
      pool.submit(() -> {
        try {
          ready.countDown();
          go.await();
          JsonNode node = read(register(sessionId, playerId));
          if ("REGISTERED".equals(node.get("result").asText())) {
            registered.incrementAndGet();
            registeredIds.add(playerId);
          } else if ("WAITLISTED".equals(node.get("result").asText())) {
            waitlisted.incrementAndGet();
            waitPositions.add(node.get("waitlistPosition").asInt());
          } else {
            errors.incrementAndGet();
          }
        } catch (Exception e) {
          errors.incrementAndGet();
        } finally {
          done.countDown();
        }
      });
    }

    assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
    go.countDown();
    assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
    pool.shutdown();

    // 恰好 capacity 人抢到名额，其余全部候补，没有任何失败/超卖
    assertThat(errors.get()).isZero();
    assertThat(registered.get()).isEqualTo(capacity);
    assertThat(waitlisted.get()).isEqualTo(players - capacity);

    // 候补顺位严格为 1..16，不重号不跳号
    assertThat(waitPositions).hasSize(players - capacity);
    Set<Integer> uniquePositions = new HashSet<>(waitPositions);
    assertThat(uniquePositions).hasSize(players - capacity);
    for (int p = 1; p <= players - capacity; p++) {
      assertThat(uniquePositions).contains(p);
    }

    JsonNode session = read(rest.getForEntity(api("/api/sessions/" + sessionId), String.class));
    assertThat(session.get("registeredCount").asInt()).isEqualTo(capacity);
    assertThat(session.get("waitlistCount").asInt()).isEqualTo(players - capacity);
    assertThat(session.get("remainingSlots").asInt()).isZero();
    assertThat(session.get("full").asBoolean()).isTrue();

    // 并发后再由一名抢到名额的玩家取消：候补首位自动转正，总数仍守恒
    long cancellingPlayer = registeredIds.iterator().next();
    JsonNode afterCancel = read(cancel(sessionId, cancellingPlayer));
    assertThat(afterCancel.get("promotedPlayerName").asText()).isNotBlank();
    assertThat(afterCancel.get("registeredCount").asInt()).isEqualTo(capacity);
    assertThat(afterCancel.get("waitlistCount").asInt()).isEqualTo(players - capacity - 1);
  }

  @Test
  void creatingSessionValidatesScriptTimeAndCapacity() {
    HttpHeaders headers = jsonHeaders();
    // 人数上限低于剧本最低人数（剧本2 最少 5 人）
    ResponseEntity<String> tooSmall = rest.postForEntity(api("/api/sessions"),
        new HttpEntity<>(Map.of(
            "scriptId", 2,
            "startTime", LocalDateTime.now().plusDays(1).toString(),
            "capacity", 2,
            "operator", "门店管理员"), headers), String.class);
    assertThat(tooSmall.getStatusCode().is4xxClientError()).isTrue();
    assertThat(read(tooSmall).get("message").asText()).contains("最低人数");

    // 开始时间已过
    ResponseEntity<String> pastTime = rest.postForEntity(api("/api/sessions"),
        new HttpEntity<>(Map.of(
            "scriptId", 2,
            "startTime", LocalDateTime.now().minusHours(1).toString(),
            "capacity", 6,
            "operator", "门店管理员"), headers), String.class);
    assertThat(pastTime.getStatusCode().is4xxClientError()).isTrue();
    assertThat(read(pastTime).get("message").asText()).contains("开始时间");

    // 剧本不存在
    ResponseEntity<String> missingScript = rest.postForEntity(api("/api/sessions"),
        new HttpEntity<>(Map.of(
            "scriptId", 9999,
            "startTime", LocalDateTime.now().plusDays(1).toString(),
            "capacity", 6,
            "operator", "门店管理员"), headers), String.class);
    assertThat(missingScript.getStatusCode().is4xxClientError()).isTrue();
    assertThat(read(missingScript).get("message").asText()).contains("剧本不存在");
  }
}
