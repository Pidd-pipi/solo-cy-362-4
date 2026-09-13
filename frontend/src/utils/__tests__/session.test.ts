import { describe, expect, it } from "vitest";
import { formatStoreDateTime } from "../instant";
import { isSessionRegisterable } from "../session";

/**
 * 报名截止“同一绝对时刻”的时区一致性单元测试。
 *
 * 后端把场次开始时间按门店时区（Asia/Shanghai）下发为带偏移的 ISO-8601，
 * 例如 2026-09-13 19:00 门店时间 => 2026-09-13T19:00:00+08:00。
 * 这是一个确定的绝对时刻（等价于 11:00Z）。测试在 TZ=Asia/Shanghai 与
 * TZ=UTC 两个进程中分别执行（见 scripts/tz-vitest.mjs），断言：
 * 无论浏览器处于哪个时区，对“同一绝对时刻”的可报名结论与展示结果完全一致，
 * 未到点不会提前禁用，到点后才截止。
 */

// 固定的同一绝对时刻：门店 19:00（+08:00），即 UTC 11:00
const START_OFFSET = "2026-09-13T19:00:00+08:00";
const START_UTC = "2026-09-13T11:00:00Z";

const openSession = {
  registerable: true,
  status: "SCHEDULED" as const,
  startTime: START_OFFSET,
};

/** 绝对毫秒：开始时刻的 epoch，用 +08:00 与用 Z 两种写法必须相等。 */
const startInstant = new Date(START_OFFSET).getTime();
const startInstantUtc = new Date(START_UTC).getTime();

describe("同一绝对时刻：+08:00 与 Z 两种写法解析一致", () => {
  it("带偏移与带 Z 的字符串解析为同一个 epoch", () => {
    expect(startInstant).toBe(startInstantUtc);
    expect(startInstant).toBe(Date.UTC(2026, 8, 13, 11, 0, 0)); // UTC 11:00
  });

  it("不同浏览器时区进程里该 epoch 恒定（本断言在两个 TZ 进程都成立）", () => {
    expect(startInstant).toBe(1_789_297_200_000); // 2026-09-13T11:00:00Z
  });
});

describe("截止边界以绝对时刻为准（与时区无关）", () => {
  // now 一律用 UTC 绝对时刻构造，避免受进程 TZ 影响
  const oneSecondBefore = new Date(startInstant - 1000);
  const atBoundary = new Date(startInstant);
  const oneSecondAfter = new Date(startInstant + 1000);
  const oneHourAfter = new Date(Date.UTC(2026, 8, 13, 12, 0, 0)); // 12:00Z = 20:00 门店
  const dayBefore = new Date(Date.UTC(2026, 8, 12, 11, 0, 0));

  it.each([
    ["边界前 1 秒", oneSecondBefore, true],
    ["前一天同一绝对时刻", dayBefore, true],
    ["恰好开始时刻（严格小于，边界点截止）", atBoundary, false],
    ["边界后 1 秒", oneSecondAfter, false],
    ["开始 1 小时后", oneHourAfter, false],
  ] as const)("%s（上海/UTC 两进程结论一致）", (_label, now, expected) => {
    expect(isSessionRegisterable(openSession, now)).toBe(expected);
  });

  it("未到点绝不提前禁用：门店本地 18:59（=10:59Z，开始前 1 分钟）可报", () => {
    const oneMinuteBeforeStoreWall = new Date(Date.UTC(2026, 8, 13, 10, 59, 0));
    expect(isSessionRegisterable(openSession, oneMinuteBeforeStoreWall)).toBe(true);
  });

  it("到点才截止：门店本地 19:00（=11:00Z）不可报，19:01 仍不可报", () => {
    expect(isSessionRegisterable(openSession, new Date(Date.UTC(2026, 8, 13, 11, 0, 0)))).toBe(false);
    expect(isSessionRegisterable(openSession, new Date(Date.UTC(2026, 8, 13, 11, 1, 0)))).toBe(false);
  });
});

describe("UTC 字面量与门店偏移字面量等价（任选服务端表达都一致）", () => {
  const sessionUtc = { ...openSession, startTime: START_UTC };

  it("开始前/后对两种写法结论完全相同", () => {
    const before = new Date(startInstant - 1);
    const after = new Date(startInstant + 1);
    expect(isSessionRegisterable(sessionUtc, before)).toBe(true);
    expect(isSessionRegisterable(openSession, before)).toBe(true);
    expect(isSessionRegisterable(sessionUtc, after)).toBe(false);
    expect(isSessionRegisterable(openSession, after)).toBe(false);
  });
});

describe("开场时间展示固定按门店时区，不随浏览器时区变化", () => {
  it("同一绝对时刻在上海/UTC 两个进程都展示为门店 19:00", () => {
    expect(formatStoreDateTime(START_OFFSET)).toBe("2026-09-13 19:00");
    expect(formatStoreDateTime(START_UTC)).toBe("2026-09-13 19:00");
  });

  it("UTC 11:00 这一时刻在门店时间是晚上 19:00（即使浏览器是零时区）", () => {
    const rendered = formatStoreDateTime("2026-09-13T11:00:00Z");
    expect(rendered).toBe("2026-09-13 19:00");
  });
});

describe("可报名结果只由绝对时刻与场次开关决定", () => {
  const before = new Date(startInstant - 1000);
  const after = new Date(startInstant + 1000);

  it("已开始/已取消场次：无论开始时刻前后都不可报名", () => {
    expect(isSessionRegisterable({ ...openSession, status: "STARTED" }, before)).toBe(false);
    expect(isSessionRegisterable({ ...openSession, status: "CANCELLED" }, before)).toBe(false);
    expect(isSessionRegisterable({ ...openSession, status: "STARTED" }, after)).toBe(false);
  });

  it("服务端 registerable=false 权威兜底：即使绝对时刻未到也不可报", () => {
    expect(isSessionRegisterable({ ...openSession, registerable: false }, before)).toBe(false);
  });

  it("非法时间串不影响服务端已开放场次（业务现有容错：true）", () => {
    expect(isSessionRegisterable({ ...openSession, startTime: "not-a-date" }, after)).toBe(true);
  });

  it("同一绝对时刻与同一 now 多次调用稳定（可重复运行）", () => {
    for (let i = 0; i < 20; i++) {
      expect(isSessionRegisterable(openSession, before)).toBe(true);
      expect(isSessionRegisterable(openSession, after)).toBe(false);
    }
  });
});
