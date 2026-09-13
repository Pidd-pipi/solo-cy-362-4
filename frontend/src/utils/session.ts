import type { SessionItem } from "../types";
import { parseInstant } from "./instant";

/**
 * 场次当前是否仍可报名。
 *
 * 同时满足：服务端标记 registerable（场次未取消/开始、未到点）与“当前绝对时刻”
 * 仍早于开始时刻。开始时间由后端按门店时区下发为带偏移的 ISO-8601（同一绝对时刻），
 * 这里直接比较 epoch 毫秒，因此页面在任意浏览器时区下结论一致：
 * 未到点不会提前禁用，到点后才截止。服务端标记是权威兜底。
 */
export function isSessionRegisterable(
  session: Pick<SessionItem, "registerable" | "startTime" | "status">,
  now: Date,
): boolean {
  if (!session.registerable || session.status !== "SCHEDULED") {
    return false;
  }
  const startInstant = parseInstant(session.startTime);
  return Number.isNaN(startInstant) ? true : now.getTime() < startInstant;
}
